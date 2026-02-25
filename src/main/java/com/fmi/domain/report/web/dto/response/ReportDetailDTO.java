package com.fmi.domain.report.web.dto.response;

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
@Schema(description = "신고 상세 응답")
public class ReportDetailDTO {
    @Schema(description = "신고 ID", example = "1")
    private Long reportId;
    @Schema(description = "신고 대상 타입", example = "POST")
    private ReportTargetType targetType;
    @Schema(description = "신고 대상 ID", example = "1")
    private Long targetId;
    @Schema(description = "신고 대상의 제목/내용 일부", example = "게시글 제목...")
    private String targetTitle;
    @Schema(description = "신고 타입", example = "SPAM")
    private ReportType reportType;
    @Schema(description = "신고 사유 내용", example = "스팸 게시글입니다.")
    private String reason;
    @Schema(description = "처리 상태", example = "PENDING")
    private ReportStatus status;
    @Schema(description = "답변 여부", example = "false")
    private Boolean answered;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "처리 완료 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime resolvedAt;
}
