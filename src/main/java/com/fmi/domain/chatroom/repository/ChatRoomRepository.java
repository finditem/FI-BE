package com.fmi.domain.chatroom.repository;

import com.fmi.domain.chatroom.data.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByPostIdAndUserId(Long postId, Long contactUserId);

    @Query("select cr from ChatRoom cr " +
            "join fetch cr.user " +
            "join fetch cr.post p " +
            "join fetch p.user " +
            "where (cr.user.id = :userId or p.user.id = :userId) " +
            "and cr.lastMessage is not null " +
            "and (cr.updatedAt < :cursorUpdatedAt or (cr.updatedAt = :cursorUpdatedAt and cr.id < :cursorId)) " +
            "order by cr.updatedAt desc, cr.id desc")
    Slice<ChatRoom> findMyChatRoomsWithCursor(
            @Param("userId") Long userId,
            @Param("cursorUpdatedAt") LocalDateTime cursorUpdatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("select cr from ChatRoom cr " +
            "join fetch cr.user " +
            "join fetch cr.post p " +
            "join fetch p.user " +
            "where (cr.user.id = :userId or p.user.id = :userId) " +
            "and cr.lastMessage is not null " +
            "order by cr.updatedAt desc, cr.id desc")
    Slice<ChatRoom> findMyChatRoomsFirstPage(
            @Param("userId") Long userId,
            Pageable pageable);
}
