package com.fmi.domain.user.web.dto;

import com.fmi.domain.Enum.WithdrawalReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AccountDeleteRequest {
    @Schema(description = "탈퇴 사유 (최대 3개)", example = "[\"NOT_USING\", \"DIFFICULT_TO_USE\"]", required = true)
    @NotEmpty(message = "탈퇴 사유를 최소 1개 선택해주세요")
    @Size(max = 3, message = "탈퇴 사유는 최대 3개까지 선택 가능합니다")
    private List<WithdrawalReason> reasons;

    @Schema(description = "기타 사유 (reasons에 OTHER가 포함된 경우 필수)", example = "직접 입력한 사유")
    private String otherReason;
}
