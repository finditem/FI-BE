package com.fmi.domain.report.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.report.converter.ReportConverter;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.report.web.dto.request.ReportCreateRequestDTO;
import com.fmi.domain.report.web.dto.response.ReportListDTO;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportConverter reportConverter;
    
    /**
     * 신고하기 (통합)
     */
    @Transactional
    public Long createReport(ReportCreateRequestDTO request, User user) {
        // 중복 신고 확인
        reportRepository.findByReporterAndTargetTypeAndTargetId(
                user, request.getTargetType(), request.getTargetId())
                .ifPresent(report -> {
                    throw new GeneralException(ErrorStatus.REPORT_ALREADY_EXISTS);
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
                        .orElseThrow(() -> new GeneralException(ErrorStatus.COMMENT_NOT_FOUND));
                break;
            case USER:
                // UserRepository 조회 필요
                break;
            case CHAT:
                // ChatMessage 조회 필요
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
                    return "사용자 " + targetId;
                case CHAT:
                    return "채팅 메시지";
                default:
                    return "알 수 없음";
            }
        } catch (Exception e) {
            return "조회 실패";
        }
    }
}

