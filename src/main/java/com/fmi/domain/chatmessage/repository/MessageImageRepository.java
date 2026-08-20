package com.fmi.domain.chatmessage.repository;

import com.fmi.domain.chatmessage.data.MessageImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageImageRepository extends JpaRepository<MessageImage, Long> {
    List<MessageImage> findAllByChatMessage_IdIn(List<Long> messageIds);
}
