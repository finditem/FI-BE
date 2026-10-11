package com.fmi.domain.review.data;

import lombok.Getter;

@Getter
public enum ReviewHelpType {
    KIND_AND_WARM("친절하고 따뜻하게 대해주셨어요."),
    TRUSTWORTHY_COMMUNICATION("믿고 소통할 수 있었어요."),
    QUICK_RESPONSE("빠르게 연락해 주셔서 도움이 됐어요."),
    SAFE_STORAGE("물건을 안전하게 보관해 주셨어요."),
    USEFUL_INFORMATION("물건을 찾을 수 있도록 정보를 알려주셨어요.");

    private final String label;

    ReviewHelpType(String label) {
        this.label = label;
    }
}
