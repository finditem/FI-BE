package com.fmi.domain.findcompletion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.findcompletion.exception.FindCompletionErrorStatus;
import com.fmi.domain.findcompletion.repository.FindCompletionRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.repository.NotificationRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("FindCompletionService")
class FindCompletionServiceTest extends IntegrationTestSupport {

    @Autowired
    private FindCompletionService findCompletionService;

    @Autowired
    private FindCompletionRepository findCompletionRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostFavoriteRepository postFavoriteRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("채팅방에서 찾기 완료를 처리할 때")
    class DescribeComplete {

        @Nested
        @DisplayName("분실물 게시글 작성자가 요청하면")
        class ContextWithLostPostAuthor {

            @Test
            @DisplayName("채팅 상대를 도움을 준 사용자로 저장하고 게시글을 찾음 상태로 바꾼다")
            void itSavesCompletionAndMarksPostFound() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("완료작성자")
                        .email("completion-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("완료도움자")
                        .email("completion-helper@test.com")
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
                ChatRoom chatRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                chatRoom.initFromPost(post);
                chatRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                chatRoom.addParticipant(
                        ChatRoomParticipant.builder().user(helper).build());
                chatRoomRepository.save(chatRoom);

                // when
                FindCompletion result = findCompletionService.complete(chatRoom.getId(), author.getEmail());

                // then
                FindCompletion saved =
                        findCompletionRepository.findById(result.getId()).orElseThrow();
                assertThat(saved.getSourcePostId()).isEqualTo(post.getId());
                assertThat(saved.isCompletedIn(chatRoom.getId())).isTrue();
                assertThat(saved.getRequester().getId()).isEqualTo(author.getId());
                assertThat(saved.getHelper().getId()).isEqualTo(helper.getId());
                assertThat(postRepository.findById(post.getId()).orElseThrow().getPostStatus())
                        .isEqualTo(PostStatus.FOUND);
            }

            @Test
            @DisplayName("게시글을 즐겨찾기한 사용자에게 상태 변경 알림을 저장한다")
            void itNotifiesFavoriteUsers() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("알림작성자")
                        .email("notify-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("알림도움자")
                        .email("notify-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User favoriteUser = userRepository.save(User.builder()
                        .nickname("즐겨찾기사용자")
                        .email("notify-favorite@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "가방을 잃어버렸어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.LOST,
                        Category.ELECTRONICS,
                        "갈색 가방입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));
                postFavoriteRepository.save(PostFavorite.create(favoriteUser, post));
                ChatRoom chatRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                chatRoom.initFromPost(post);
                chatRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                chatRoom.addParticipant(
                        ChatRoomParticipant.builder().user(helper).build());
                chatRoomRepository.save(chatRoom);

                // when
                findCompletionService.complete(chatRoom.getId(), author.getEmail());

                // then
                assertThat(notificationRepository.findAllByUserAndReferenceIdAndTypeIn(
                                favoriteUser, post.getId(), List.of(NotificationType.FAVORITE)))
                        .hasSize(1);
            }
        }

        @Nested
        @DisplayName("게시글 작성자가 아닌 채팅 참여자가 요청하면")
        class ContextWithNonAuthor {

            @Test
            @DisplayName("ACCESS_DENIED 예외를 던진다")
            void itThrowsAccessDenied() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("권한작성자")
                        .email("auth-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("권한도움자")
                        .email("auth-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "우산을 잃어버렸어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.LOST,
                        Category.ELECTRONICS,
                        "파란 우산입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));
                ChatRoom chatRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                chatRoom.initFromPost(post);
                chatRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                chatRoom.addParticipant(
                        ChatRoomParticipant.builder().user(helper).build());
                chatRoomRepository.save(chatRoom);

                // when & then
                assertThatThrownBy(() -> findCompletionService.complete(chatRoom.getId(), helper.getEmail()))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(FindCompletionErrorStatus.ACCESS_DENIED);
            }
        }

        @Nested
        @DisplayName("습득물 게시글의 채팅방이면")
        class ContextWithFoundPost {

            @Test
            @DisplayName("INVALID_POST_TYPE 예외를 던진다")
            void itThrowsInvalidPostType() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("습득작성자")
                        .email("found-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User owner = userRepository.save(User.builder()
                        .nickname("습득주인")
                        .email("found-owner@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "지갑을 주웠어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.FOUND,
                        Category.ELECTRONICS,
                        "검은색 지갑입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));
                ChatRoom chatRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                chatRoom.initFromPost(post);
                chatRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                chatRoom.addParticipant(
                        ChatRoomParticipant.builder().user(owner).build());
                chatRoomRepository.save(chatRoom);

                // when & then
                assertThatThrownBy(() -> findCompletionService.complete(chatRoom.getId(), author.getEmail()))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(FindCompletionErrorStatus.INVALID_POST_TYPE);
            }
        }

        @Nested
        @DisplayName("같은 게시글의 다른 채팅방에서 이미 완료했으면")
        class ContextWithCompletedPost {

            @Test
            @DisplayName("ALREADY_COMPLETED 예외를 던지고 완료 기록을 추가하지 않는다")
            void itThrowsAlreadyCompleted() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("중복작성자")
                        .email("duplicate-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User firstHelper = userRepository.save(User.builder()
                        .nickname("첫도움자")
                        .email("duplicate-first@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User secondHelper = userRepository.save(User.builder()
                        .nickname("둘째도움자")
                        .email("duplicate-second@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "열쇠를 잃어버렸어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.LOST,
                        Category.ELECTRONICS,
                        "자동차 열쇠입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));
                ChatRoom firstRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                firstRoom.initFromPost(post);
                firstRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                firstRoom.addParticipant(
                        ChatRoomParticipant.builder().user(firstHelper).build());
                chatRoomRepository.save(firstRoom);
                ChatRoom secondRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 12, 0))
                        .build();
                secondRoom.initFromPost(post);
                secondRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                secondRoom.addParticipant(
                        ChatRoomParticipant.builder().user(secondHelper).build());
                chatRoomRepository.save(secondRoom);
                findCompletionService.complete(firstRoom.getId(), author.getEmail());

                // when & then
                assertThatThrownBy(() -> findCompletionService.complete(secondRoom.getId(), author.getEmail()))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(FindCompletionErrorStatus.ALREADY_COMPLETED);
                assertThat(findCompletionRepository.findBySourcePostId(post.getId()))
                        .get()
                        .extracting(completion -> completion.isCompletedIn(firstRoom.getId()))
                        .isEqualTo(true);
            }
        }

        @Nested
        @DisplayName("같은 게시글의 두 채팅방에서 동시에 요청하면")
        class ContextWithConcurrentRequests {

            @Test
            @Transactional(propagation = Propagation.NOT_SUPPORTED)
            @DisplayName("한 요청만 완료하고 나머지는 ALREADY_COMPLETED 예외를 던진다")
            void itCompletesOnlyOnce() throws Exception {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("동시작성자")
                        .email("concurrent-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User firstHelper = userRepository.save(User.builder()
                        .nickname("동시첫도움자")
                        .email("concurrent-first@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User secondHelper = userRepository.save(User.builder()
                        .nickname("동시둘째도움자")
                        .email("concurrent-second@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "휴대폰을 잃어버렸어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.LOST,
                        Category.ELECTRONICS,
                        "흰색 휴대폰입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));
                ChatRoom firstRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                firstRoom.initFromPost(post);
                firstRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                firstRoom.addParticipant(
                        ChatRoomParticipant.builder().user(firstHelper).build());
                chatRoomRepository.save(firstRoom);
                ChatRoom secondRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 12, 0))
                        .build();
                secondRoom.initFromPost(post);
                secondRoom.addParticipant(
                        ChatRoomParticipant.builder().user(author).build());
                secondRoom.addParticipant(
                        ChatRoomParticipant.builder().user(secondHelper).build());
                chatRoomRepository.save(secondRoom);

                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<FindCompletion>> futures = new ArrayList<>();
                try {
                    for (Long roomId : List.of(firstRoom.getId(), secondRoom.getId())) {
                        Callable<FindCompletion> task = () -> {
                            start.await();
                            return findCompletionService.complete(roomId, author.getEmail());
                        };
                        futures.add(executor.submit(task));
                    }

                    // when
                    start.countDown();
                    int successCount = 0;
                    List<Throwable> failures = new ArrayList<>();
                    for (Future<FindCompletion> future : futures) {
                        try {
                            future.get();
                            successCount++;
                        } catch (ExecutionException e) {
                            failures.add(e.getCause());
                        }
                    }

                    // then
                    assertThat(successCount).isEqualTo(1);
                    assertThat(failures)
                            .singleElement()
                            .isInstanceOf(GeneralException.class)
                            .extracting("code")
                            .isEqualTo(FindCompletionErrorStatus.ALREADY_COMPLETED);
                    assertThat(findCompletionRepository.findBySourcePostId(post.getId()))
                            .isPresent();
                } finally {
                    executor.shutdownNow();
                    findCompletionRepository
                            .findBySourcePostId(post.getId())
                            .ifPresent(findCompletionRepository::delete);
                    chatRoomRepository.deleteAll(List.of(firstRoom, secondRoom));
                    postRepository.delete(post);
                    userRepository.deleteAll(List.of(author, firstHelper, secondHelper));
                }
            }
        }
    }
}
