package com.fmi.domain.report.service;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.report.converter.ReportConverter;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.ReportAnswerImage;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.data.enums.ReportType;
import com.fmi.domain.report.event.ReportEvent;
import com.fmi.domain.report.repository.ReportAnswerImageRepository;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.report.web.dto.request.ReportCreateRequestDTO;
import com.fmi.domain.report.web.dto.response.ReportDetailDTO;
import com.fmi.domain.report.web.dto.response.ReportListDTO;
import com.fmi.domain.userblock.service.BlockService;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.EmailService;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ReportConverter reportConverter;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ReportAnswerImageRepository reportAnswerImageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BlockService blockService;

    /**
     * 신고하기 (통합)
     */
    @Transactional
    public Long createReport(ReportCreateRequestDTO request, UserDetails userDetails) {
        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 관리자 신고 차단
        if (user.getRole() == Role.ADMIN) {
            throw new GeneralException(ErrorStatus._REPORT_ADMIN_NOT_ALLOWED);
        }

        // 중복 신고 확인
        reportRepository
                .findByReporterAndTargetTypeAndTargetId(user, request.getTargetType(), request.getTargetId())
                .ifPresent(report -> {
                    throw new GeneralException(ErrorStatus._REPORT_ALREADY_EXISTS);
                });

        // 신고 대상 존재 여부 확인
        validateTargetExists(request.getTargetType(), request.getTargetId(), user);

        // 신고 생성
        Report report = Report.builder()
                .reporter(user)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reportType(request.getReportType())
                .reason(request.getReason())
                .build();

        Report saved = reportRepository.save(report);

        // 채팅 신고면 상대방 차단
        autoBlockOnChatReport(request, user);

        eventPublisher.publishEvent(ReportEvent.from(saved, user));

        // 신고 접수 이메일 발송 (신고자에게)
        try {
            String targetTitle = getTargetTitle(saved.getTargetType(), saved.getTargetId());
            String nickname = user.getNickname() != null ? user.getNickname() : "회원";
            String reportDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                    .format(saved.getCreatedAt() != null ? saved.getCreatedAt() : java.time.LocalDateTime.now());
            String reportContent = saved.getReason() != null ? saved.getReason() : "";

            emailService.sendHtmlEmailAsync(
                    user.getEmail(),
                    "신고가 접수되었습니다",
                    "report-received-email.html",
                    java.util.Map.of(
                            "NAME", nickname,
                            "TITLE", targetTitle,
                            "USER", user.getEmail(),
                            "DATE", reportDate,
                            "CONTENT", reportContent));
        } catch (Exception e) {
            log.error("신고 접수 이메일 발송 실패: reportId={}", saved.getReportId(), e);
        }

        return saved.getReportId();
    }

    /**
     * 내 신고 내역 조회
     */
    public Page<ReportListDTO> getMyReports(User user, ReportStatus status, Boolean answered, Pageable pageable) {
        Page<Report> reports;

        if (status != null && answered != null) {
            reports = reportRepository.findByReporterAndStatusAndAnswered(user, status, answered, pageable);
        } else if (status != null) {
            reports = reportRepository.findByReporterAndStatus(user, status, pageable);
        } else if (answered != null) {
            reports = reportRepository.findByReporterAndAnswered(user, answered, pageable);
        } else {
            reports = reportRepository.findByReporter(user, pageable);
        }

        return reports.map(report -> {
            String targetTitle = getTargetTitle(report.getTargetType(), report.getTargetId());
            return reportConverter.toListDTO(report, targetTitle);
        });
    }

    /**
     * 내 신고 내역 조회 (커서 기반)
     */
    public CursorPageResponse<ReportListDTO> getMyReportsCursor(
            User user, ReportStatus status, Boolean answered, String keyword, Long cursor, int size) {
        int fetchSize = size + 1;
        Pageable limit = PageRequest.of(0, fetchSize);
        List<Report> reports;

        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            String lower = trimmed.toLowerCase();
            List<String> matchedTypes = Arrays.stream(ReportType.values())
                    .filter(t -> t.name().toLowerCase().contains(lower)
                            || t.getDescription().toLowerCase().contains(lower))
                    .map(Enum::name)
                    .toList();
            if (matchedTypes.isEmpty()) {
                matchedTypes = List.of("__NONE__");
            }
            reports = reportRepository.findByReporterWithKeywordCursor(
                    user.getId(),
                    status != null ? status.name() : null,
                    answered,
                    trimmed,
                    matchedTypes,
                    cursor,
                    fetchSize);
        } else if (status != null && answered != null) {
            reports = reportRepository.findByReporterAndStatusAndAnsweredCursor(user, status, answered, cursor, limit);
        } else if (status != null) {
            reports = reportRepository.findByReporterAndStatusCursor(user, status, cursor, limit);
        } else if (answered != null) {
            reports = reportRepository.findByReporterAndAnsweredCursor(user, answered, cursor, limit);
        } else {
            reports = reportRepository.findByReporterCursor(user, cursor, limit);
        }

        boolean hasNext = reports.size() > size;
        List<Report> content = hasNext ? reports.subList(0, size) : reports;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getReportId() : null;

        List<ReportListDTO> responseList = content.stream()
                .map(report -> {
                    String targetTitle = getTargetTitle(report.getTargetType(), report.getTargetId());
                    return reportConverter.toListDTO(report, targetTitle);
                })
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }

    /**
     * 내 신고 상세 조회
     */
    public ReportDetailDTO getMyReportDetail(User user, Long reportId) {
        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._REPORT_NOT_FOUND));

        if (!report.getReporter().getId().equals(user.getId())) {
            throw new GeneralException(ErrorStatus._REPORT_ACCESS_DENIED);
        }

        String targetTitle = getTargetTitle(report.getTargetType(), report.getTargetId());
        return reportConverter.toDetailDTO(report, targetTitle);
    }

    /**
     * 신고 대상 존재 확인
     */
    private void validateTargetExists(ReportTargetType targetType, Long targetId, User reporter) {
        switch (targetType) {
            case POST:
                postRepository.findById(targetId).orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));
                break;
            case COMMENT:
                commentRepository
                        .findById(targetId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));
                break;
            case USER:
                userRepository
                        .findActiveById(targetId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
                break;
            case CHAT:
                ChatRoom room = chatRoomRepository
                        .findById(targetId)
                        .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));
                if (!room.isParticipant(reporter)) {
                    throw new GeneralException(ErrorStatus._CHATROOM_ACCESS_DENIED);
                }
                break;
        }
    }

    /**
     * 신고 대상 제목 가져오기
     */
    public String getTargetTitle(ReportTargetType targetType, Long targetId) {
        try {
            switch (targetType) {
                case POST:
                    return postRepository.findById(targetId).map(Post::getTitle).orElse("삭제된 게시글");
                case COMMENT:
                    return commentRepository
                            .findById(targetId)
                            .map(comment -> comment.getContent()
                                            .substring(
                                                    0,
                                                    Math.min(
                                                            50,
                                                            comment.getContent().length()))
                                    + "...")
                            .orElse("삭제된 댓글");
                case USER:
                    return userRepository
                            .findActiveById(targetId)
                            .map(u -> u.getNickname() + " 사용자")
                            .orElse("삭제된 사용자");
                case CHAT:
                    return chatRoomRepository
                            .findById(targetId)
                            .map(room -> "채팅방: " + room.getId())
                            .orElse("삭제된 채팅방");
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
    public void updateStatus(Long reportId, ReportStatus status) {
        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._REPORT_NOT_FOUND));

        switch (status) {
            case PENDING:
                report.pending();
                break;
            case REVIEWED:
                report.review();
                break;
            case RESOLVED:
                report.resolve();
                break;
        }

        // 신고자에게 상태 변경 알림
        User reporter = report.getReporter();
        if (reporter != null) {
            notificationService.createNotification(
                    reporter,
                    NotificationType.REPORT_RESULT,
                    "신고에 대한 답변이 등록되었습니다.",
                    "",
                    ReferenceType.REPORT,
                    report.getReportId());
        }
    }

    /**
     * 신고 답변 작성(관리자)
     */
    @Transactional
    public void answerReport(Long reportId, String adminAnswer, List<String> imageUrls, UserDetails userDetails) {
        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._REPORT_NOT_FOUND));

        User adminUser = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        report.answer(adminAnswer, adminUser);

        // 기존 답변 이미지 삭제 후 새로 저장
        reportAnswerImageRepository.deleteAll(reportAnswerImageRepository.findByReportReportId(reportId));

        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<ReportAnswerImage> images = imageUrls.stream()
                    .map(url -> ReportAnswerImage.create(report, url))
                    .toList();
            reportAnswerImageRepository.saveAll(images);
        }

        // 신고자에게 답변 알림 및 이메일 발송
        User reporter = report.getReporter();
        if (reporter != null) {
            notificationService.createNotification(
                    reporter,
                    NotificationType.REPORT_RESULT,
                    "신고에 대한 답변이 등록되었습니다",
                    adminAnswer,
                    ReferenceType.REPORT,
                    report.getReportId());

            try {
                String targetTitle = getTargetTitle(report.getTargetType(), report.getTargetId());
                String reporterNickname = reporter.getNickname() != null ? reporter.getNickname() : "회원";
                String reportDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                        .format(report.getCreatedAt() != null ? report.getCreatedAt() : java.time.LocalDateTime.now());

                emailService.sendHtmlEmailAsync(
                        reporter.getEmail(),
                        "신고 답변 안내",
                        "report-result-email.html",
                        java.util.Map.of(
                                "name", reporterNickname,
                                "TITLE", targetTitle,
                                "USER", reporter.getEmail(),
                                "RESULT", "답변 완료",
                                "DATE", reportDate,
                                "CONTENT", adminAnswer));
            } catch (Exception e) {
                log.error("신고 답변 이메일 발송 실패: reportId={}", report.getReportId(), e);
            }
        }

        // 피신고자에게 신고 조치 안내 이메일 발송
        try {
            User targetUser = findTargetUser(report.getTargetType(), report.getTargetId());
            if (targetUser != null) {
                String targetNickname = targetUser.getNickname() != null ? targetUser.getNickname() : "회원";
                String categoryName =
                        report.getTargetType() != null ? report.getTargetType().getDescription() : "";
                emailService.sendHtmlEmailAsync(
                        targetUser.getEmail(),
                        "신고 처리 결과 안내",
                        "report-notification-email.html",
                        java.util.Map.of(
                                "name", targetNickname,
                                "CATEGORY", categoryName,
                                "USER", targetUser.getEmail(),
                                "NICKNAME", targetNickname));
            }
        } catch (Exception e) {
            log.error("피신고자 이메일 발송 실패: reportId={}", report.getReportId(), e);
        }
    }

    private User findTargetUser(ReportTargetType targetType, Long targetId) {
        try {
            switch (targetType) {
                case POST:
                    return postRepository.findById(targetId).map(Post::getUser).orElse(null);
                case COMMENT:
                    return commentRepository
                            .findById(targetId)
                            .map(c -> c.getUser())
                            .orElse(null);
                case USER:
                    return userRepository.findActiveById(targetId).orElse(null);
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("피신고자 조회 실패: targetType={}, targetId={}", targetType, targetId, e);
            return null;
        }
    }

    /**
     * 채팅 신고면 상대방 차단
     */
    private void autoBlockOnChatReport(ReportCreateRequestDTO request, User reporter) {
        if (request.getTargetType() != ReportTargetType.CHAT) {
            return;
        }

        ChatRoom room = chatRoomRepository
                .findById(request.getTargetId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));

        User targetUser = room.getOtherParticipant(reporter.getId());

        blockService.block(reporter.getId(), targetUser.getId());
    }
}
