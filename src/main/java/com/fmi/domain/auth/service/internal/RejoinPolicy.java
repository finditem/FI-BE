package com.fmi.domain.auth.service.internal;

import com.fmi.domain.auth.data.RejoinType;
import com.fmi.domain.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RejoinPolicy {

    private static final long REJOIN_BLOCK_DAYS = 7;

    private final Clock clock;
    private final Set<String> kakaoTestAccountEmails;

    public RejoinPolicy(
            Clock clock,
            @Value("#{'${auth.rejoin.kakao-test-account-emails:}'.split(',').![#this.trim()].?[!#this.isEmpty()]}")
                    Set<String> kakaoTestAccountEmails) {
        this.clock = clock;
        this.kakaoTestAccountEmails = kakaoTestAccountEmails;
    }

    public void validate(User user, RejoinType rejoinType) {
        LocalDateTime rejoinBlockedSince = LocalDateTime.now(clock).minusDays(REJOIN_BLOCK_DAYS);
        boolean recentlyWithdrawn = user.getDeletedAt().isAfter(rejoinBlockedSince);
        if (!recentlyWithdrawn) {
            return;
        }

        if (isExemptKakaoTestAccount(user, rejoinType)) {
            return;
        }

        throw new GeneralException(ErrorStatus._EMAIL_RECENTLY_DELETED);
    }

    private boolean isExemptKakaoTestAccount(User user, RejoinType rejoinType) {
        if (rejoinType != RejoinType.KAKAO) {
            return false;
        }

        return kakaoTestAccountEmails.contains(user.getEmail());
    }
}
