package com.fmi.domain.user.web.dto.response;

import com.fmi.domain.user.data.enums.LanguageCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferredLanguageResponse {

    private LanguageCode preferredLanguage;
}
