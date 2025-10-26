package com.fmi.domain.chatroom.repository;

import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant,Long> {
    boolean existsByUser_IdAndChatRoom_Id(Long userId, Long roomId);
    Optional<ChatRoomParticipant> findByUser_IdAndChatRoom_Id(Long userId, Long roomId);

    Optional<ChatRoomParticipant> findByChatRoom_IdAndUser_Id(Long roomId, Long userId);

    List<ChatRoomParticipant> findAllByChatRoom_Id(Long roomId);

    // 내 채팅방 목록 조회 (첫 페이지)
    @Query("SELECT pt FROM ChatRoomParticipant pt " +
            " JOIN FETCH pt.chatRoom cr " +
            " JOIN FETCH cr.post p " +
            " JOIN FETCH pt.lastMessage lm " + // lastMessage 정보
            " JOIN cr.participants otherPt " + // 상대방 participant 정보
            " JOIN otherPt.user otherUser " + // 상대방 user 정보
            " WHERE pt.user.id = :userId " +
            "   AND otherPt.user.id != :userId " +
            "   AND pt.participantState = com.fmi.domain.chatroom.data.enums.ParticipantState.ACTIVE " + //
            // ACTIVE 상태
            "   AND pt.lastMessage IS NOT NULL " + // lastMessage가 있음
            " ORDER BY pt.lastMessageSentAt DESC, pt.id DESC")
    Slice<ChatRoomParticipant> findMyChatRoomsFirstPage(
            @Param("userId") Long userId,
            Pageable pageable);

    // 내 채팅방 목록 조회 (커서 기반)
    @Query("SELECT pt FROM ChatRoomParticipant pt " +
            " JOIN FETCH pt.chatRoom cr " +
            " JOIN FETCH cr.post p " +
            " JOIN FETCH pt.lastMessage lm " +
            " JOIN cr.participants otherPt " +
            " JOIN otherPt.user otherUser " +
            " WHERE pt.user.id = :userId " +
            "   AND otherPt.user.id != :userId " +
            "   AND pt.participantState = com.fmi.domain.chatroom.data.enums.ParticipantState.ACTIVE " +
            "   AND pt.lastMessage IS NOT NULL " +
            // 커서 로직 (lastMessageSentAt, pt.id 기준)
            "   AND (pt.lastMessageSentAt < :cursorTime OR (pt.lastMessageSentAt = :cursorTime AND pt.id < :cursorId)) " +
            " ORDER BY pt.lastMessageSentAt DESC, pt.id DESC")
    Slice<ChatRoomParticipant> findMyChatRoomsWithCursor(
            @Param("userId") Long userId,
            @Param("cursorTime") LocalDateTime cursorTime,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
