package com.fmi.domain.report.web.dto.request;

import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.data.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequestDTO {
    
    @NotNull(message = "신고 대상 타입을 선택해주세요.")
    private ReportTargetType targetType;
    
    @NotNull(message = "신고 대상 ID를 입력해주세요.")
    @Positive(message = "올바른 ID를 입력해주세요.")
    private Long targetId;
    
    @NotNull(message = "신고 유형을 선택해주세요.")
    private ReportType reportType;
    
    @NotBlank(message = "신고 사유를 입력해주세요.")
    @Size(min = 10, max = 1000, message = "신고 사유는 10자 이상 1000자 이하로 입력해주세요.")
    private String reason;
}

