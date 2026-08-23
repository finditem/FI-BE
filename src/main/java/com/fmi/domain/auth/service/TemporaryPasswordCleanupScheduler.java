package com.fmi.domain.auth.service;

import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TemporaryPasswordCleanupScheduler {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTemporaryPasswords() {
        LocalDateTime now = LocalDateTime.now();
        List<User> usersWithExpiredTempPassword = userRepository.findUsersWithExpiredTemporaryPassword(now);

        for (User user : usersWithExpiredTempPassword) {
            user.restoreExpiredTemporaryPassword(now);
            userRepository.save(user);
        }
    }
}
