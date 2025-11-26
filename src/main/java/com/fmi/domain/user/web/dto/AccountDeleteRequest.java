package com.fmi.domain.user.web.dto;

import com.fmi.domain.Enum.WithdrawalReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountDeleteRequest {
    @Schema(description = "탈퇴 사유", example = "NOT_USING", required = true)
    @NotNull(message = "탈퇴 사유를 선택해주세요")
    private WithdrawalReason reason;
    
    @Schema(description = "기타 사유 (reason이 OTHER인 경우)", example = "직접 입력한 사유")
    private String otherReason;
}

