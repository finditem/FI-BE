package com.fmi.domain.post.web.dto.response;

import com.fmi.domain.Enum.LanguageCode;
import lombok.*;

@Getter
@Builder
public class PostTranslationResponse {

    private Long postId;
    private LanguageCode languageCode;
    private String translatedTitle;
    private String translatedContent;
}
