package com.fmi.domain.auth.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 만료된 임시 비밀번호를 정리하는 스케줄러
 * 매 시간마다 실행되어 만료된 임시 비밀번호를 제거하고 원래 비밀번호를 복원합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryPasswordCleanupScheduler {

    private final UserRepository userRepository;

    /**
     * 매 시간마다 실행 (정각)
     * 만료된 임시 비밀번호를 제거하고 원래 비밀번호를 복원합니다.
     */
    @Scheduled(cron = "0 0 * * * *") // 매 시간 정각 (0분)
    @Transactional
    public void cleanupExpiredTemporaryPasswords() {
        LocalDateTime now = LocalDateTime.now();
        
        // 만료된 임시 비밀번호가 있는 사용자 조회
        List<User> usersWithExpiredTempPassword = userRepository.findUsersWithExpiredTemporaryPassword(now);
        
        if (usersWithExpiredTempPassword.isEmpty()) {
            log.debug("만료된 임시 비밀번호가 없습니다.");
            return;
        }
        
        int cleanedCount = 0;
        for (User user : usersWithExpiredTempPassword) {
            try {
                // 원래 비밀번호 복원
                if (user.getOriginalPassword() != null) {
                    user.setPassword(user.getOriginalPassword());
                    user.setOriginalPassword(null);
                }
                
                // 임시 비밀번호 제거
                user.setTemporaryPassword(null);
                user.setTemporaryPasswordExpiresAt(null);
                user.setUpdatedAt(LocalDateTime.now());
                
                userRepository.save(user);
                cleanedCount++;
                
                log.debug("사용자 ID {}의 만료된 임시 비밀번호를 정리하고 원래 비밀번호를 복원했습니다.", user.getId());
            } catch (Exception e) {
                log.error("사용자 ID {}의 임시 비밀번호 정리 중 오류 발생: {}", user.getId(), e.getMessage(), e);
            }
        }
        
        log.info("만료된 임시 비밀번호 {}개를 정리했습니다.", cleanedCount);
    }
}

