package com.fmi.domain.chatmessage.data;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatmessage.data.enums.MessageType;
import com.fmi.domain.chatroom.data.ChatRoom;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content")
    private String content;

    //@Column(name = "readornot", nullable = false)
    private boolean readornot;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="chatRoom_id", nullable=false)
    private ChatRoom chatRoom;

    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MessageImage> images = new ArrayList<>();

    public void addImage(MessageImage image) {
        this.images.add(image);
        image.setChatMessage(this);
    }

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
