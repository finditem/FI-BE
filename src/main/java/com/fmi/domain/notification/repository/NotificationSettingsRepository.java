package com.fmi.domain.notification.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notification.data.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {
    
    // 사용자별 알림 설정 조회
    Optional<NotificationSettings> findByUser(User user);
}

