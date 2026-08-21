package com.fmi.domain.notification.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notification.data.Notification;
import com.fmi.domain.notification.data.enums.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자별 알림 목록 조회
    Page<Notification> findByUser(User user, Pageable pageable);

    // 읽지 않은 알림 목록
    Page<Notification> findByUserAndIsReadFalse(User user, Pageable pageable);

    // 읽지 않은 알림 개수
    long countByUserAndIsReadFalse(User user);

    // 사용자의 모든 알림 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsRead(@Param("user") User user);

    // 사용자의 모든 알림 삭제
    void deleteByUser(User user);

    // 타입별 알림 조회
    Page<Notification> findByUserAndType(User user, NotificationType type, Pageable pageable);

    // 타입별 읽지 않은 알림 조회
    Page<Notification> findByUserAndTypeAndIsReadFalse(User user, NotificationType type, Pageable pageable);

    Optional<Notification> findByUserAndReferenceIdAndType(
            User receiver, Long referenceId, NotificationType notificationType);

    Optional<Notification> findByUserAndReferenceIdAndRoomIdAndType(
            User receiver, Long referenceId, Long roomId, NotificationType notificationType);

    // 채팅 알림 읽음 처리용 (CHAT + CHAT_REMINDER)
    List<Notification> findAllByUserAndReferenceIdAndTypeIn(User user, Long referenceId, List<NotificationType> types);

    // 커서 기반 조회 - 전체
    @Query("""
            SELECT n FROM Notification n
            WHERE n.user = :user
              AND (:cursor IS NULL OR n.notificationId < :cursor)
            ORDER BY n.notificationId DESC
            """)
    List<Notification> findByUserCursor(@Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);

    // 커서 기반 조회 - 읽지 않은 알림
    @Query("""
            SELECT n FROM Notification n
            WHERE n.user = :user AND n.isRead = false
              AND (:cursor IS NULL OR n.notificationId < :cursor)
            ORDER BY n.notificationId DESC
            """)
    List<Notification> findByUserAndIsReadFalseCursor(
            @Param("user") User user, @Param("cursor") Long cursor, Pageable pageable);

    // 커서 기반 조회 - 타입별
    @Query("""
            SELECT n FROM Notification n
            WHERE n.user = :user AND n.type = :type
              AND (:cursor IS NULL OR n.notificationId < :cursor)
            ORDER BY n.notificationId DESC
            """)
    List<Notification> findByUserAndTypeCursor(
            @Param("user") User user,
            @Param("type") NotificationType type,
            @Param("cursor") Long cursor,
            Pageable pageable);

    // 커서 기반 조회 - 타입별 + 읽지 않은 알림
    @Query("""
            SELECT n FROM Notification n
            WHERE n.user = :user AND n.type = :type AND n.isRead = false
              AND (:cursor IS NULL OR n.notificationId < :cursor)
            ORDER BY n.notificationId DESC
            """)
    List<Notification> findByUserAndTypeAndIsReadFalseCursor(
            @Param("user") User user,
            @Param("type") NotificationType type,
            @Param("cursor") Long cursor,
            Pageable pageable);
}
