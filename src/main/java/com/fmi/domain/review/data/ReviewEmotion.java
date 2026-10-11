package com.fmi.domain.review.data;

import lombok.Getter;

@Getter
public enum ReviewEmotion {
    MOVED("감동했어요"),
    GRATEFUL("감사했어요"),
    HEART_FLUTTERED("심쿵했어요");

    private final String label;

    ReviewEmotion(String label) {
        this.label = label;
    }
}
