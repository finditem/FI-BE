package com.fmi.domain.chatroom.repository;

import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ChatRoomParticipantRepositoryCustom {
    Slice<ChatRoomParticipant> findMyChatRooms(Long userId, Long cursorId, Pageable pageable);
}
