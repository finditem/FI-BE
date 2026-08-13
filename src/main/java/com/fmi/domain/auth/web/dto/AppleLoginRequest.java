package com.fmi.domain.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppleLoginRequest {

    @NotBlank(message = "code 필수")
    private String code;

    @Pattern(regexp = "^(dev|release|prod)$", message = "environment는 dev, release, prod 중 하나여야 합니다")
    private String environment;
}
