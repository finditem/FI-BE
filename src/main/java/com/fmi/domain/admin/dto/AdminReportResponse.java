package com.fmi.domain.admin.dto;

import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.data.enums.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 신고 응답")
public class AdminReportResponse {

    @Schema(description = "신고 ID", example = "1")
    private Long reportId;
    @Schema(description = "신고 대상 타입", example = "POST")
    private ReportTargetType targetType;
    @Schema(description = "신고 대상 ID", example = "1")
    private Long targetId;
    @Schema(description = "신고 타입", example = "SPAM")
    private ReportType reportType;
    @Schema(description = "처리 상태", example = "PENDING")
    private ReportStatus status;
    @Schema(description = "신고 사유", example = "스팸 게시글입니다.")
    private String reason;
    @Schema(description = "관리자 메모", example = "검토 완료")
    private String adminNote;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "수정 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime updatedAt;
    @Schema(description = "처리 완료 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime resolvedAt;

    @Schema(description = "신고자 ID", example = "1")
    private Long reporterId;
    @Schema(description = "신고자 닉네임", example = "홍길동")
    private String reporterNickname;
    @Schema(description = "신고자 이메일", example = "reporter@example.com")
    private String reporterEmail;
}

