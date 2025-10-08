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
    Optional<ChatRoom> findByPost_PostIdAndUser_UserId(Long postId, Long contactUserId);

    @Query("select cr from ChatRoom cr " +
            "where cr.post.id = :postId " +
            "and cr.lastMessage is not null " +
            "and (cr.updatedAt < :cursorUpdatedAt or (cr.updatedAt = :cursorUpdatedAt and cr.chatroom_id < :cursorId)) " +
            "order by cr.updatedAt desc, cr.chatroom_id desc")
    Slice<ChatRoom> findMyPostChatRoomsWithCursor(
            @Param("postId") Long postId,
            @Param("cursorUpdatedAt") LocalDateTime cursorUpdatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("select cr from ChatRoom cr " +
            "where cr.post.id = :postId " +
            "and cr.lastMessage is not null " +
            "order by cr.updatedAt desc, cr.chatroom_id desc")
    Slice<ChatRoom> findMyPostChatRoomsFirstPage(
            @Param("postId") Long postId,
            Pageable pageable);
}
