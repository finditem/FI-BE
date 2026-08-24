package com.fmi.domain.post.converter.util;

import com.fmi.domain.post.data.PostTranslation;
import com.fmi.domain.post.web.dto.response.PostTranslationResponse;

public final class PostTranslationConverter {
    public static PostTranslationResponse translate(PostTranslation translation) {
        return PostTranslationResponse.builder()
                .postId(translation.getPost().getId())
                .languageCode(translation.getLanguageCode())
                .translatedTitle(translation.getTranslatedTitle())
                .translatedContent(translation.getTranslatedContent())
                .build();
    }

    private PostTranslationConverter() {}
}
