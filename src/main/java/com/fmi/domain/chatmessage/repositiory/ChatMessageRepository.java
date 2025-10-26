package com.fmi.domain.chatmessage.repositiory;

import com.fmi.domain.chatmessage.data.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 첫 페이지 조회 (커서가 없을 때)
     */
    @Query("SELECT m FROM ChatMessage m " +
            " WHERE m.chatRoom.id = :roomId " +
            " and ( :cutoff is null or m.id> :cutoff )" +
            " ORDER BY m.id DESC")
    Slice<ChatMessage> findByChatRoomIdOrderByIdDesc(
            @Param("roomId") Long roomId, @Param("cutoff") Long cutoff,
            Pageable pageable
    );

    /**
     * 커서가 있을 때
     */
    @Query("SELECT m FROM ChatMessage m " +
            " WHERE m.chatRoom.id = :roomId AND m.id < :cursorId " +
            " and ( :cutoff is null or m.id> :cutoff )" +
            " ORDER BY m.id DESC")
    Slice<ChatMessage> findByRoomIdWithCursor(
            @Param("roomId") Long roomId,
            @Param("cursorId") Long cursorId, @Param("cutoff") Long cutoff,
            Pageable pageable
    );

    Optional<ChatMessage> findTopByChatRoom_IdOrderByIdDesc(Long roomId);

}
