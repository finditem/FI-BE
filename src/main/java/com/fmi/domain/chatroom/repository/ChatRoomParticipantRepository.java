package com.fmi.domain.chatroom.repository;

import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant,Long>, ChatRoomParticipantRepositoryCustom {
    boolean existsByUser_IdAndChatRoom_Id(Long userId, Long roomId);
    Optional<ChatRoomParticipant> findByUser_IdAndChatRoom_Id(Long userId, Long roomId);

    Optional<ChatRoomParticipant> findByChatRoom_IdAndUser_Id(Long roomId, Long userId);

    List<ChatRoomParticipant> findAllByChatRoom_Id(Long roomId);

}
