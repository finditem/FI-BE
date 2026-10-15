package com.fmi.domain.user.service;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserStatisticsRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserStatisticsRecorder {
    private final UserStatisticsRepository repository;

    public void recordSignup(User user, LocalDateTime occurredAt) {
        if (!user.isStatisticsMember()) return;
        repository.increment(occurredAt.toLocalDate(), 1, 0);
    }

    public void recordWithdrawal(User user, LocalDateTime occurredAt) {
        if (user.getRole() != Role.USER || !user.isPrivacyPolicyAgreed() || !user.isTermsOfServiceAgreed()) return;
        repository.increment(occurredAt.toLocalDate(), 0, 1);
    }
}
