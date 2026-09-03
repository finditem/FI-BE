package com.fmi.domain.user.response;

import com.fmi.domain.Enum.LanguageCode;
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
