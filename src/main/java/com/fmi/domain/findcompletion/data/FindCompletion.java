package com.fmi.domain.findcompletion.data;

import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.user.data.User;
import com.fmi.global.data.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "find_completion",
        uniqueConstraints = @UniqueConstraint(name = "uk_find_completion_source_post", columnNames = "source_post_id"))
public class FindCompletion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_post_id", nullable = false, updatable = false)
    private Long sourcePostId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", updatable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", updatable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id", updatable = false)
    private User helper;

    @Builder
    private FindCompletion(Long sourcePostId, ChatRoom chatRoom, User requester, User helper) {
        this.sourcePostId = sourcePostId;
        this.chatRoom = chatRoom;
        this.requester = requester;
        this.helper = helper;
    }
}
