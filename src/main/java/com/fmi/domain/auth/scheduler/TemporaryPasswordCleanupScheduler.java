package com.fmi.domain.auth.scheduler;

import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import java.time.Clock;
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
    private final Clock clock;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTemporaryPasswords() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<User> usersWithExpiredTempPassword = userRepository.findUsersWithExpiredTemporaryPassword(now);

        for (User user : usersWithExpiredTempPassword) {
            user.restoreExpiredTemporaryPassword(now);
            userRepository.save(user);
        }
    }
}
