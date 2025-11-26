package com.fmi.domain.Enum;

import lombok.Getter;

@Getter
public enum WithdrawalReason {
    NOT_USING("잘 사용하지 않아요"),
    LOW_TRUST("서비스에 대한 신뢰도가 낮아요"),
    DIFFICULT_TO_USE("사용이 어려워요"),
    DUPLICATE_ACCOUNT("다른 계정이 있어요"),
    UNPLEASANT_USER("불쾌감을 주는 사용자를 만났어요"),
    UNFAIR_RESTRICTION("억울하게 서비스 이용이 제한됐어요"),
    OTHER("기타");

    private final String description;

    WithdrawalReason(String description) {
        this.description = description;
    }
}

