package com.fmi.domain.chat.room.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.chat.message.data.ChatMessage;
import com.fmi.domain.chat.message.data.enums.MessageType;
import com.fmi.domain.chat.message.repository.ChatMessageRepository;
import com.fmi.domain.chat.room.data.ChatRoom;
import com.fmi.domain.chat.room.data.ChatRoomParticipant;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.data.enums.Radius;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("ChatRoomRepository")
class ChatRoomRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("게시글의 메시지가 있는 채팅방 수를 셀 때")
    class DescribeCountChatRoomsWithMessageByPostId {

        @Nested
        @DisplayName("메시지 없는 채팅방과 다른 게시글의 채팅방이 있으면")
        class ContextWithEmptyRoomAndOtherPostRoom {

            @Test
            @DisplayName("해당 게시글에서 메시지가 있는 채팅방만 센다")
            void itCountsOnlyRoomsWithMessageOfPost() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("게시글채팅작성자")
                        .email("post-chat-count-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User messagedUser = userRepository.save(User.builder()
                        .nickname("메시지보낸사용자")
                        .email("post-chat-count-messaged@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User silentUser = userRepository.save(User.builder()
                        .nickname("메시지없는사용자")
                        .email("post-chat-count-silent@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "지갑을 잃어버렸어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.LOST,
                        Category.WALLET,
                        "검은색 지갑입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));
                Post otherPost = postRepository.save(Post.create(
                        "우산을 주웠어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.FOUND,
                        Category.ETC,
                        "파란색 우산입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));

                ChatRoom messagedRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                messagedRoom.initFromPost(post);
                messagedRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                messagedRoom.addParticipant(
                        ChatRoomParticipant.builder().user(messagedUser).build());
                chatRoomRepository.save(messagedRoom);
                chatMessageRepository.save(ChatMessage.builder()
                        .user(messagedUser)
                        .chatRoom(messagedRoom)
                        .content("지갑 찾았어요")
                        .messageType(MessageType.TEXT)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 5))
                        .build());

                ChatRoom emptyRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 12, 0))
                        .build();
                emptyRoom.initFromPost(post);
                emptyRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                emptyRoom.addParticipant(
                        ChatRoomParticipant.builder().user(silentUser).build());
                chatRoomRepository.save(emptyRoom);

                ChatRoom otherPostRoom = ChatRoom.builder()
                        .post(otherPost)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 13, 0))
                        .build();
                otherPostRoom.initFromPost(otherPost);
                otherPostRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                otherPostRoom.addParticipant(
                        ChatRoomParticipant.builder().user(messagedUser).build());
                chatRoomRepository.save(otherPostRoom);
                chatMessageRepository.save(ChatMessage.builder()
                        .user(messagedUser)
                        .chatRoom(otherPostRoom)
                        .content("제 우산 같아요")
                        .messageType(MessageType.TEXT)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 13, 5))
                        .build());
                chatMessageRepository.flush();

                // when
                long result = chatRoomRepository.countChatRoomsWithMessageByPostId(post.getId());

                // then
                assertThat(result).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("메시지가 있던 채팅방에서 참여자가 모두 나갔으면")
        class ContextWithAllParticipantsLeft {

            @Test
            @DisplayName("나간 채팅방도 센다")
            void itCountsRoomsAllParticipantsLeft() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("나간방작성자")
                        .email("post-chat-count-left-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User contactUser = userRepository.save(User.builder()
                        .nickname("나간방상대")
                        .email("post-chat-count-left-contact@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "카드를 잃어버렸어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.LOST,
                        Category.WALLET,
                        "파란색 카드지갑입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));
                ChatRoom chatRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                chatRoom.initFromPost(post);
                ChatRoomParticipant authorParticipant =
                        ChatRoomParticipant.builder().user(author).build();
                ChatRoomParticipant contactParticipant =
                        ChatRoomParticipant.builder().user(contactUser).build();
                chatRoom.addParticipant(authorParticipant);
                chatRoom.addParticipant(contactParticipant);
                chatRoomRepository.save(chatRoom);
                ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                        .user(contactUser)
                        .chatRoom(chatRoom)
                        .content("제 카드지갑 같아요")
                        .messageType(MessageType.TEXT)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 5))
                        .build());
                authorParticipant.leftChatRoom(message.getId());
                contactParticipant.leftChatRoom(message.getId());
                chatRoomRepository.flush();

                // when
                long result = chatRoomRepository.countChatRoomsWithMessageByPostId(post.getId());

                // then
                assertThat(result).isEqualTo(1);
            }
        }
    }
}
