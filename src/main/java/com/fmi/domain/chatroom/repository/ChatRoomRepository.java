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
    //Optional<ChatRoom> findByPostIdAndUserId(Long postId, Long contactUserId);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p1 JOIN cr.participants p2 " +
            "WHERE cr.post.id = :postId AND p1.user.id = :userId1 AND p2.user.id = :userId2")
    Optional<ChatRoom> findChatRoomByPostAndUsers(@Param("postId") Long postId,
                                                  @Param("userId1") Long userId1,
                                                  @Param("userId2") Long userId2);

    @Query("SELECT cr FROM ChatRoom cr " +
            " JOIN FETCH cr.post p "+
            " JOIN cr.participants pt " +
            " WHERE pt.user.id = :userId " +
            " AND (cr.updatedAt < :cursorUpdatedAt OR (cr.updatedAt = :cursorUpdatedAt AND cr.id < :cursorId)) " +
            " AND cr.lastMessage IS NOT NULL " +
            " ORDER BY cr.updatedAt DESC, cr.id DESC")
    Slice<ChatRoom> findMyChatRoomsWithCursor(
            @Param("userId") Long userId,
            @Param("cursorUpdatedAt") LocalDateTime cursorUpdatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("SELECT cr FROM ChatRoom cr " +
            " JOIN FETCH cr.post p " +
            " JOIN cr.participants pt" +
            " WHERE pt.user.id = :userId " +
            " AND cr.lastMessage IS NOT NULL " +
            " ORDER BY cr.updatedAt DESC, cr.id DESC")
    Slice<ChatRoom> findMyChatRoomsFirstPage(
            @Param("userId") Long userId,
            Pageable pageable);
}
