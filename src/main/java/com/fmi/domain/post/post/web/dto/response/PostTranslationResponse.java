package com.fmi.domain.post.post.web.dto.response;

import com.fmi.domain.user.user.data.enums.LanguageCode;
import lombok.*;

@Getter
@Builder
public class PostTranslationResponse {

    private Long postId;
    private LanguageCode languageCode;
    private String translatedTitle;
    private String translatedContent;
}
