package com.fmi.domain.chatroom.repository;

import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.post.data.enums.PostType;
import com.fmi.global.enums.SortType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ChatRoomParticipantRepositoryCustom {
    Slice<ChatRoomParticipant> findMyChatRooms(
            Long userId, Long cursorId, Pageable pageable, PostType type, String address, SortType sort);
}
