package com.fmi.domain.chat.room.repository;

import com.fmi.domain.chat.room.data.ChatRoomParticipant;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.global.enums.SortType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ChatRoomParticipantRepositoryCustom {
    Slice<ChatRoomParticipant> findMyChatRooms(
            Long userId, Long cursorId, Pageable pageable, PostType type, String address, SortType sort);
}
