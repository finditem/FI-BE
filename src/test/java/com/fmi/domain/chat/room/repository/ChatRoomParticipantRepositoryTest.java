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
@DisplayName("ChatRoomParticipantRepository")
class ChatRoomParticipantRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("사용자의 참여 중인 채팅방 수를 셀 때")
    class DescribeCountActiveChatRoomsByUser {

        @Nested
        @DisplayName("메시지 없이 생성만 된 채팅방이 있으면")
        class ContextWithChatRoomWithoutMessage {

            @Test
            @DisplayName("메시지가 있는 채팅방만 센다")
            void itCountsOnlyChatRoomsWithMessage() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("채팅수작성자")
                        .email("chat-count-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User messagedUser = userRepository.save(User.builder()
                        .nickname("메시지보낸사용자")
                        .email("chat-count-messaged@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User silentUser = userRepository.save(User.builder()
                        .nickname("메시지없는사용자")
                        .email("chat-count-silent@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "지갑을 잃어버렸어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.LOST,
                        Category.ELECTRONICS,
                        "검은색 지갑입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));

                ChatRoom messagedRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                messagedRoom.initFromPost(post);
                ChatRoomParticipant authorInMessagedRoom =
                        ChatRoomParticipant.builder().user(author).build();
                ChatRoomParticipant messagedUserParticipant =
                        ChatRoomParticipant.builder().user(messagedUser).build();
                messagedRoom.addParticipant(authorInMessagedRoom);
                messagedRoom.addParticipant(messagedUserParticipant);
                chatRoomRepository.save(messagedRoom);
                ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                        .user(messagedUser)
                        .chatRoom(messagedRoom)
                        .content("지갑 찾았어요")
                        .messageType(MessageType.TEXT)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 5))
                        .build());
                authorInMessagedRoom.updateLastMessage(message);
                messagedUserParticipant.updateLastMessage(message);

                ChatRoom silentRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 12, 0))
                        .build();
                silentRoom.initFromPost(post);
                silentRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                silentRoom.addParticipant(
                        ChatRoomParticipant.builder().user(silentUser).build());
                chatRoomRepository.save(silentRoom);
                chatRoomRepository.flush();

                // when
                long result = chatRoomParticipantRepository.countActiveChatRoomsByUser(author);

                // then
                assertThat(result).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("메시지가 있던 채팅방을 나갔으면")
        class ContextWithLeftChatRoom {

            @Test
            @DisplayName("나간 채팅방은 세지 않는다")
            void itExcludesLeftChatRooms() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("나간방작성자")
                        .email("chat-count-left-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User contactUser = userRepository.save(User.builder()
                        .nickname("나간방상대")
                        .email("chat-count-left-contact@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "우산을 주웠어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.FOUND,
                        Category.ELECTRONICS,
                        "파란색 우산입니다.",
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
                        .content("제 우산 같아요")
                        .messageType(MessageType.TEXT)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 5))
                        .build());
                authorParticipant.updateLastMessage(message);
                contactParticipant.updateLastMessage(message);
                authorParticipant.leftChatRoom(message.getId());
                chatRoomRepository.flush();

                // when
                long result = chatRoomParticipantRepository.countActiveChatRoomsByUser(author);

                // then
                assertThat(result).isZero();
            }
        }
    }
}
