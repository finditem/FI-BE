package com.fmi.domain.user.user.web.dto.request;

import com.fmi.domain.user.user.data.enums.LanguageCode;
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
