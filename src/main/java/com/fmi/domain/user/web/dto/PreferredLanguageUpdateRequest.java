package com.fmi.domain.user.web.dto;

import com.fmi.domain.Enum.LanguageCode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PreferredLanguageUpdateRequest {

    @NotNull(message = "선호 언어는 필수입니다") private LanguageCode preferredLanguage;
}
