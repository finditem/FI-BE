package com.fmi.domain.report.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.chatmessage.repository.ChatMessageRepository;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.report.converter.ReportConverter;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.event.ReportEvent;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.report.web.dto.request.ReportCreateRequestDTO;
import com.fmi.domain.report.web.dto.response.ReportListDTO;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReportConverter reportConverter;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 신고하기 (통합)
     */
    @Transactional
    public Long createReport(ReportCreateRequestDTO request, UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 중복 신고 확인
        reportRepository.findByReporterAndTargetTypeAndTargetId(
                user, request.getTargetType(), request.getTargetId())
                .ifPresent(report -> {
                    throw new GeneralException(ErrorStatus._REPORT_ALREADY_EXISTS);
                });
        
        // 신고 대상 존재 여부 확인
        validateTargetExists(request.getTargetType(), request.getTargetId());
        
        // 신고 생성
        Report report = Report.builder()
                .reporter(user)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reportType(request.getReportType())
                .reason(request.getReason())
                .build();
        
        Report saved = reportRepository.save(report);

        eventPublisher.publishEvent(ReportEvent.from(saved, user));

        // 신고 접수 이메일 발송 (신고자에게)
        try {
            String targetTitle = getTargetTitle(saved.getTargetType(), saved.getTargetId());
            String nickname = user.getNickname() != null ? user.getNickname() : "회원";
            String reportDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                    .format(saved.getCreatedAt() != null ? saved.getCreatedAt() : java.time.LocalDateTime.now());
            String reportContent = saved.getReason() != null ? saved.getReason() : "";
            
            emailService.sendHtmlEmail(
                user.getEmail(),
                "신고가 접수되었습니다",
                "report-received-email.html",
                java.util.Map.of(
                    "NAME", nickname,
                    "TITLE", targetTitle,
                    "USER", targetTitle, // 신고 대상 정보
                    "DATE", reportDate,
                    "CONTENT", reportContent
                )
            );
        } catch (Exception e) {
            // 이메일 발송 실패해도 신고는 성공 처리
        }
        
        return saved.getReportId();
    }
    
    /**
     * 내 신고 내역 조회
     */
    public Page<ReportListDTO> getMyReports(User user, ReportStatus status, Pageable pageable) {
        Page<Report> reports;
        
        if (status != null) {
            reports = reportRepository.findByReporterAndStatus(user, status, pageable);
        } else {
            reports = reportRepository.findByReporter(user, pageable);
        }
        
        return reports.map(report -> {
            String targetTitle = getTargetTitle(report.getTargetType(), report.getTargetId());
            return reportConverter.toListDTO(report, targetTitle);
        });
    }
    
    /**
     * 신고 대상 존재 확인
     */
    private void validateTargetExists(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case POST:
                postRepository.findById(targetId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));
                break;
            case COMMENT:
                commentRepository.findById(targetId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));
                break;
            case USER:
                userRepository.findActiveById(targetId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
                break;
            case CHAT:
                chatMessageRepository.findById(targetId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND)); // ChatMessage 용 에러 추가 필요
                break;
        }
    }
    
    /**
     * 신고 대상 제목 가져오기
     */
    private String getTargetTitle(ReportTargetType targetType, Long targetId) {
        try {
            switch (targetType) {
                case POST:
                    return postRepository.findById(targetId)
                            .map(Post::getTitle)
                            .orElse("삭제된 게시글");
                case COMMENT:
                    return commentRepository.findById(targetId)
                            .map(comment -> comment.getContent().substring(0, Math.min(50, comment.getContent().length())) + "...")
                            .orElse("삭제된 댓글");
                case USER:
                    return userRepository.findActiveById(targetId)
                            .map(u -> u.getNickname() + " 사용자")
                            .orElse("삭제된 사용자");
                case CHAT:
                    return chatMessageRepository.findById(targetId)
                            .map(msg -> "채팅: " + msg.getContent().substring(0, Math.min(30, msg.getContent().length())) + "...")
                            .orElse("삭제된 채팅");
                default:
                    return "알 수 없음";
            }
        } catch (Exception e) {
            return "조회 실패";
        }
    }

    /**
     * 신고 상태 업데이트(관리자)
     */
    @Transactional
    public void updateStatus(Long reportId, ReportStatus status, String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._REPORT_NOT_FOUND));

        // 관리자는 REVIEWED(처리중) 또는 RESOLVED(처리완료) 상태로 변경 가능
        switch (status) {
            case REVIEWED:
                report.review(adminNote);
                break;
            case RESOLVED:
                report.resolve(adminNote);
                break;
            default:
                throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        // 신고자에게 결과 알림 및 이메일 발송
        User reporter = report.getReporter();
        if (reporter != null) {
            String statusText = status == ReportStatus.RESOLVED ? "처리 완료" : "처리 중";
            String title = "신고 처리 결과: " + statusText;
            String message = adminNote == null ? "" : adminNote;
            notificationService.createNotification(
                    reporter,
                    NotificationType.REPORT_RESULT,
                    title,
                    message,
                    ReferenceType.REPORT,
                    report.getReportId()
            );
            
            // 신고 결과 이메일 발송
            try {
                String targetTitle = getTargetTitle(report.getTargetType(), report.getTargetId());
                String reportDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                        .format(report.getCreatedAt() != null ? report.getCreatedAt() : java.time.LocalDateTime.now());
                String resultText = status == ReportStatus.RESOLVED ? "처리 완료" : "처리 중";
                String content = adminNote != null && !adminNote.isEmpty() 
                        ? adminNote 
                        : (status == ReportStatus.RESOLVED ? "처리 완료되었습니다." : "처리 중입니다.");
                
                String emailSubject = status == ReportStatus.RESOLVED 
                        ? "신고 처리 결과 안내" 
                        : "신고 처리 중 안내";
                emailService.sendHtmlEmail(
                    reporter.getEmail(),
                    emailSubject,
                    "report-result-email.html",
                    java.util.Map.of(
                        "TITLE", targetTitle,
                        "USER", reporter.getEmail(),
                        "RESULT", resultText,
                        "DATE", reportDate,
                        "CONTENT", content
                    )
                );
            } catch (Exception e) {
                // 이메일 발송 실패해도 알림은 성공 처리
            }
        }
    }
}

