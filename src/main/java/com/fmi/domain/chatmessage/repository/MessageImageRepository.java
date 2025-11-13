package com.fmi.domain.chatmessage.repository;

import com.fmi.domain.chatmessage.data.MessageImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageImageRepository extends JpaRepository<MessageImage, Long> {
    List<MessageImage> findAllByChatMessage_IdIn(List<Long> messageIds);
}
