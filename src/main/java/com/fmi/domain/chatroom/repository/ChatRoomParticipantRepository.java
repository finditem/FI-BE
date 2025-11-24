package com.fmi.domain.chatroom.repository;

import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant,Long>, ChatRoomParticipantRepositoryCustom {
    boolean existsByUser_IdAndChatRoom_Id(Long userId, Long roomId);
    Optional<ChatRoomParticipant> findByUser_IdAndChatRoom_Id(Long userId, Long roomId);

    Optional<ChatRoomParticipant> findByChatRoom_IdAndUser_Id(Long roomId, Long userId);

    List<ChatRoomParticipant> findAllByChatRoom_Id(Long roomId);

    @Query("SELECT cp FROM ChatRoomParticipant cp " +
           "JOIN FETCH cp.user u " +
           "LEFT JOIN FETCH u.notificationSettings " +
           "WHERE cp.unreadCount > 0 " +
           "AND cp.firstUnreadAt <= :threshold " +
           "AND cp.lastRemindedAt IS NULL")
    List<ChatRoomParticipant> findParticipantsForReminder(@Param("threshold") LocalDateTime threshold);
}
