package com.fmi.domain.chatmessage.repositiory;

import com.fmi.domain.chatmessage.data.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
