package com.fmi.domain.post.service;

import static org.assertj.core.api.Assertions.*;

import com.fmi.domain.Enum.*;
import com.fmi.domain.post.data.*;
import com.fmi.domain.post.repository.*;
import com.fmi.domain.post.web.dto.request.*;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.external.storage.S3Service;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.support.IntegrationTestSupport;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("PostStatisticsService")
class PostStatisticsServiceTest extends IntegrationTestSupport {
    @Autowired
    S3Service s3;

    @Autowired
    PostStatisticsService service;

    @Autowired
    PostStatisticsRecorder recorder;

    @Autowired
    PostService posts;

    @Autowired
    PostRepository repository;

    @Autowired
    UserRepository users;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactions;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.reset(s3);
        jdbc.execute("DROP TABLE IF EXISTS user_statistics");
        jdbc.execute("DROP TABLE IF EXISTS post_statistics");
        try (var c = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(
                    c, new ClassPathResource("db/migration/V16__create_user_and_post_statistics.sql"));
        }
        jdbc.update(
                "INSERT INTO post_statistics (statistics_date,started_at,opening_post_count) VALUES ('2026-09-08','2026-09-08 12:00:00',100)");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM user_statistics");
        jdbc.update("DELETE FROM post_statistics");
    }

    @Nested
    @DisplayName("게시글 변동을 조회할 때")
    class DescribeGet {
        @Test
        @DisplayName("삭제 후 작성 수를 유지하고 시작일 이전의 증감과 기초 수를 총수에 반영한다")
        void itKeepsPublicationsAfterRemoval() {
            var tx = new TransactionTemplate(transactions);
            tx.executeWithoutResult(status -> {
                recorder.recordPublication(PostType.LOST, LocalDateTime.of(2026, 9, 8, 13, 0));
                recorder.recordPublication(PostType.FOUND, LocalDateTime.of(2026, 9, 9, 12, 0));
                recorder.recordRemoval(1, LocalDateTime.of(2026, 9, 9, 12, 0));
            });
            assertThat(service.get(LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 9)))
                    .isEqualTo(new PostStatisticsResult(101, 0, 1));
            assertThat(service.get(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new PostStatisticsResult(101, 1, 1));
        }

        @Test
        @DisplayName("조회 가능 날짜를 검사하고 미초기화 상태는 거부한다")
        void itChecksAvailabilityAndDates() {
            assertThatThrownBy(() -> service.get(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> assertThat(
                                    ((GeneralException) e).getErrorReason().getMessage())
                            .contains("도입일"));
            assertThatThrownBy(() -> service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 9)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> assertThat(
                                    ((GeneralException) e).getErrorReason().getMessage())
                            .contains("시작일"));
            assertThatThrownBy(() -> service.get(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 11)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> assertThat(
                                    ((GeneralException) e).getErrorReason().getMessage())
                            .contains("미래"));
            jdbc.update("DELETE FROM post_statistics");
            assertThatThrownBy(() -> service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> assertThat(
                                    ((GeneralException) e).getErrorReason().getMessage())
                            .contains("초기화"));
        }
    }

    @Nested
    @DisplayName("게시글 업무 흐름을 실행할 때")
    class DescribePublication {
        @Test
        @DisplayName("공개 작성과 삭제를 기록하고 동시 삭제는 한 번만 집계한다")
        void itRecordsCreationAndConcurrentDeletionOnce() throws Exception {
            User user = User.builder()
                    .email("post-stats-" + UUID.randomUUID() + "@example.com")
                    .nickname("글통계" + UUID.randomUUID().toString().substring(0, 8))
                    .password("test-password")
                    .role(Role.USER)
                    .build();
            users.saveAndFlush(user);
            var principal = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                    .password("test")
                    .roles("USER")
                    .build();
            var request = new PostCreateRequest(
                    PostType.LOST,
                    "통계 게시글",
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    "서울",
                    37.5,
                    127.0,
                    "내용",
                    Radius.DISTANCE_1000,
                    Category.ETC);
            Long id = posts.createPost(request, principal, List.of()).id();
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new PostStatisticsResult(101, 1, 0));
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);
            try {
                List<Future<?>> tasks = new ArrayList<>();
                for (int i = 0; i < 2; i++)
                    tasks.add(pool.submit(() -> {
                        start.await();
                        try {
                            posts.deletePost(id, principal);
                        } catch (GeneralException e) {
                            assertThat(e.getErrorReason().getCode()).isEqualTo("POST404-NOT_FOUND");
                        }
                        return null;
                    }));
                start.countDown();
                for (var task : tasks) task.get(20, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
            }
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new PostStatisticsResult(100, 1, 0));
        }

        @Test
        @DisplayName("임시저장의 최초 공개만 기록하고 공개 취소를 거부한다")
        void itRecordsFirstPublicationAndRejectsDraftReversion() {
            User user = User.builder()
                    .email("draft-stats-" + UUID.randomUUID() + "@example.com")
                    .nickname("임시통계" + UUID.randomUUID().toString().substring(0, 8))
                    .password("test-password")
                    .role(Role.USER)
                    .build();
            users.saveAndFlush(user);
            Post draft = Post.create(
                    "임시 글",
                    "서울",
                    37.5,
                    127.0,
                    PostType.FOUND,
                    Category.ETC,
                    "내용",
                    true,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    user);
            repository.saveAndFlush(draft);
            var principal = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                    .password("test")
                    .roles("USER")
                    .build();
            var publish =
                    new PostUpdateRequest(null, null, null, null, null, null, null, false, null, null, List.of(), null);
            posts.updatePost(draft.getId(), publish, principal, List.of());
            posts.updatePost(draft.getId(), publish, principal, List.of());
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new PostStatisticsResult(101, 0, 1));
            var revert =
                    new PostUpdateRequest(null, null, null, null, null, null, null, true, null, null, List.of(), null);
            assertThatThrownBy(() -> posts.updatePost(draft.getId(), revert, principal, List.of()))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> assertThat(
                                    ((GeneralException) e).getErrorReason().getMessage())
                            .contains("임시저장"));
        }

        @Test
        @DisplayName("일괄 삭제는 공개 글만 집계하고 rollback 후 통계와 삭제 상태를 복원한다")
        void itCountsPublishedBulkRemovalAndRollsBack() {
            User user = User.builder()
                    .email("bulk-stats-" + UUID.randomUUID() + "@example.com")
                    .nickname("일괄통계" + UUID.randomUUID().toString().substring(0, 8))
                    .password("test-password")
                    .role(Role.USER)
                    .build();
            users.saveAndFlush(user);
            Post published = Post.create(
                    "공개 글",
                    "서울",
                    37.5,
                    127.0,
                    PostType.LOST,
                    Category.ETC,
                    "내용",
                    false,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    user);
            Post draft = Post.create(
                    "임시 글",
                    "서울",
                    37.5,
                    127.0,
                    PostType.FOUND,
                    Category.ETC,
                    "내용",
                    true,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    user);
            repository.saveAllAndFlush(List.of(published, draft));
            var tx = new TransactionTemplate(transactions);
            tx.executeWithoutResult(status -> {
                posts.softDeleteAllByUser(user);
                status.setRollbackOnly();
            });
            assertThat(repository.findById(published.getId()).orElseThrow().isDeleted())
                    .isFalse();
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10))
                            .totalPostCount())
                    .isEqualTo(100);
            posts.softDeleteAllByUser(user);
            posts.softDeleteAllByUser(user);
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new PostStatisticsResult(99, 0, 0));
        }
    }

    @Nested
    @DisplayName("통계 기록이 실패할 때")
    class DescribeStatisticsFailure {
        @Test
        @DisplayName("통계 SQL 저장이 실패하면 게시글 생성을 rollback한다")
        void itRollsBackPostWhenStatisticsSqlFails() {
            jdbc.update("DELETE FROM post_statistics");
            User user = User.builder()
                    .email("failure-stats-" + UUID.randomUUID() + "@example.com")
                    .nickname("실패통계" + UUID.randomUUID().toString().substring(0, 8))
                    .password("test-password")
                    .role(Role.USER)
                    .build();
            users.saveAndFlush(user);
            var principal = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                    .password("test")
                    .roles("USER")
                    .build();
            var request = new PostCreateRequest(
                    PostType.LOST,
                    "통계 게시글",
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    "서울",
                    37.5,
                    127.0,
                    "내용",
                    Radius.DISTANCE_1000,
                    Category.ETC);
            jdbc.execute("ALTER TABLE post_statistics RENAME COLUMN lost_post_count TO invalid_lost_post_count");
            try {
                assertThatThrownBy(() -> posts.createPost(request, principal, List.of()))
                        .isInstanceOf(org.springframework.dao.DataAccessException.class);
                assertThat(repository.countByUserAndDeletedFalse(user)).isZero();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM post_statistics", Long.class))
                        .isZero();
            } finally {
                jdbc.execute("ALTER TABLE post_statistics RENAME COLUMN invalid_lost_post_count TO lost_post_count");
            }
        }
    }
}
