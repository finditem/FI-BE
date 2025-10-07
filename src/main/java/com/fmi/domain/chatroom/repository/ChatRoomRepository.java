package com.fmi.domain.chatroom.repository;

import com.fmi.domain.chatroom.data.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByPost_PostIdAndUser_UserId(Long postId, Long contactUserId);
}
