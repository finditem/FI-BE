package com.fmi.domain.admin.dto;

import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.data.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 신고/문의 통합 상세 응답")
public class AdminCsDetailResponse {

    @Schema(description = "유형", example = "REPORT")
    private AdminCsType type;
    @Schema(description = "ID")
    private Long id;
    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    // --- 신고 전용 필드 ---
    @Schema(description = "신고 대상 타입 (신고 전용)")
    private ReportTargetType targetType;
    @Schema(description = "신고 대상 ID (신고 전용)")
    private Long targetId;
    @Schema(description = "신고 유형 (신고 전용)")
    private ReportType reportType;
    @Schema(description = "신고 사유 (신고 전용)")
    private String reason;
    @Schema(description = "처리 상태 (신고 전용)")
    private ReportStatus reportStatus;
    @Schema(description = "관리자 메모 (신고 전용)")
    private String adminNote;
    @Schema(description = "처리 완료 시간 (신고 전용)")
    private LocalDateTime resolvedAt;
    @Schema(description = "답변 여부 (신고 전용)")
    private Boolean answered;
    @Schema(description = "신고자 ID (신고 전용)")
    private Long reporterId;
    @Schema(description = "신고자 닉네임 (신고 전용)")
    private String reporterNickname;
    @Schema(description = "신고자 이메일 (신고 전용)")
    private String reporterEmail;

    // --- 문의 전용 필드 ---
    @Schema(description = "문의 제목 (문의 전용)")
    private String title;
    @Schema(description = "문의 내용 (문의 전용)")
    private String content;
    @Schema(description = "문의 유형 (문의 전용)")
    private InquiryType inquiryType;
    @Schema(description = "문의 처리 상태 (문의 전용)")
    private InquiryStatus inquiryStatus;
    @Schema(description = "작성자 ID (문의 전용)")
    private Long userId;
    @Schema(description = "작성자 닉네임 (문의 전용)")
    private String userNickname;
    @Schema(description = "작성자 이메일 (문의 전용)")
    private String userEmail;
    @Schema(description = "댓글 목록 (문의 전용)")
    private List<InquiryCommentResponse> comments;
}
