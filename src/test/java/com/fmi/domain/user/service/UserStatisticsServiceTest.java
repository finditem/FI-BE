package com.fmi.domain.user.service;

import static org.assertj.core.api.Assertions.*;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.Enum.WithdrawalReason;
import com.fmi.domain.auth.service.AuthService;
import com.fmi.domain.auth.service.WithdrawalService;
import com.fmi.domain.auth.service.internal.EmailVerificationStore;
import com.fmi.domain.auth.web.dto.AccountDeleteRequest;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.post.web.dto.request.PostCreateRequest;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.*;
import com.fmi.domain.user.web.dto.TermsAgreeRequest;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.support.IntegrationTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("UserStatisticsService")
class UserStatisticsServiceTest extends IntegrationTestSupport {
    @Autowired
    UserStatisticsService service;

    @Autowired
    UserStatisticsRecorder recorder;

    @Autowired
    UserService userService;

    @Autowired
    WithdrawalService withdrawalService;

    @Autowired
    PostService postService;

    @Autowired
    PostRepository posts;

    @Autowired
    AuthService authService;

    @Autowired
    EmailVerificationStore verification;

    @Autowired
    UserRepository users;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactions;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("DROP TABLE IF EXISTS user_statistics");
        jdbc.execute("DROP TABLE IF EXISTS post_statistics");
        try (var c = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(
                    c, new ClassPathResource("db/migration/V15__create_user_and_post_statistics.sql"));
        }
        jdbc.update(
                "INSERT INTO user_statistics (statistics_date,started_at,opening_user_count) VALUES ('2026-09-08','2026-09-08 12:00:00',100)");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM user_statistics");
        jdbc.update("DELETE FROM post_statistics");
    }

    @Nested
    @DisplayName("기간 통계를 조회할 때")
    class DescribeGet {
        @Test
        @DisplayName("기초 수와 시작일 이전 변동을 총수에만 포함하고 빈 기간은 0을 반환한다")
        void itIncludesOpeningAndEarlierChangesOnlyInTotal() {
            jdbc.update(
                    "UPDATE user_statistics SET signup_count=5,withdrawal_count=2 WHERE statistics_date='2026-09-08'");
            jdbc.update(
                    "INSERT INTO user_statistics (statistics_date,signup_count,withdrawal_count) VALUES ('2026-09-09',3,1)");
            assertThat(service.get(LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 9)))
                    .isEqualTo(new UserStatisticsResult(105, 3, 1));
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new UserStatisticsResult(105, 0, 0));
        }

        @Test
        @DisplayName("도입 전, 역전, 미래 날짜를 거부하고 같은 날짜와 오늘을 허용한다")
        void itChecksDateBoundaries() {
            assertThatThrownBy(() -> service.get(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> assertThat(
                                    ((GeneralException) e).getErrorReason().getMessage())
                            .contains("도입일"));
            assertThatThrownBy(() -> service.get(LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 8)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> assertThat(
                                    ((GeneralException) e).getErrorReason().getMessage())
                            .contains("시작일"));
            assertThatThrownBy(() -> service.get(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 11)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> assertThat(
                                    ((GeneralException) e).getErrorReason().getMessage())
                            .contains("미래"));
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10))
                            .totalUserCount())
                    .isEqualTo(100);
        }

        @Test
        @DisplayName("초기화 전에는 통계 조회를 거부한다")
        void itRejectsUninitializedRead() {
            jdbc.update("DELETE FROM user_statistics");
            assertThatThrownBy(() -> service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isInstanceOf(GeneralException.class)
                    .satisfies(e -> assertThat(
                                    ((GeneralException) e).getErrorReason().getMessage())
                            .contains("초기화"));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_statistics", Long.class))
                    .isZero();
        }

        @Test
        @DisplayName("초기값 0도 초기화로 인식하며 도입 시각은 일별 증가 후에도 보존한다")
        void itPreservesZeroOpeningAndStartTime() {
            jdbc.update("UPDATE user_statistics SET opening_user_count=0");
            User user = User.builder()
                    .role(Role.USER)
                    .privacyPolicyAgreed(true)
                    .termsOfServiceAgreed(true)
                    .build();
            new TransactionTemplate(transactions)
                    .executeWithoutResult(status -> recorder.recordSignup(user, LocalDateTime.of(2026, 9, 8, 13, 0)));
            assertThat(service.get(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 8)))
                    .isEqualTo(new UserStatisticsResult(1, 1, 0));
            assertThat(jdbc.queryForObject(
                            "SELECT started_at FROM user_statistics WHERE statistics_date='2026-09-08'",
                            LocalDateTime.class))
                    .isEqualTo(LocalDateTime.of(2026, 9, 8, 12, 0));
        }
    }

    @Nested
    @DisplayName("회원 상태 변경을 기록할 때")
    class DescribeRecord {
        @Test
        @DisplayName("이메일 가입 완료를 기록하고 업무 실패 시 회원과 통계를 함께 rollback한다")
        void itRecordsSignupAndRollsBackWithUser() {
            String email = "stats-" + UUID.randomUUID() + "@example.com";
            SignupRequest request = new SignupRequest();
            request.setEmail(email);
            request.setNickname("통계" + UUID.randomUUID().toString().substring(0, 8));
            request.setPassword("Password123!");
            request.setPrivacyPolicyAgreed(true);
            request.setTermsOfServiceAgreed(true);
            request.setContentPolicyAgreed(false);
            request.setMarketingConsent(false);
            verification.markVerified(email);
            var tx = new TransactionTemplate(transactions);
            tx.executeWithoutResult(status -> {
                authService.signup(request);
                status.setRollbackOnly();
            });
            assertThat(users.findByEmail(email)).isEmpty();
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10))
                            .signupCount())
                    .isZero();
            User saved = authService.signup(request);
            assertThat(saved.isStatisticsMember()).isTrue();
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new UserStatisticsResult(101, 1, 0));
        }

        @Test
        @DisplayName("동시 약관 동의를 한 번 집계하고 회원 정보 삭제 후에도 과거 수치를 유지한다")
        void itCountsConcurrentTermsOnceAndPreservesAfterDeletion() throws Exception {
            User user = User.builder()
                    .email("social-stats-" + UUID.randomUUID() + "@example.com")
                    .nickname("소셜" + UUID.randomUUID().toString().substring(0, 8))
                    .password("test-password")
                    .role(Role.USER)
                    .build();
            users.saveAndFlush(user);
            TermsAgreeRequest request = new TermsAgreeRequest();
            request.setPrivacyPolicyAgreed(true);
            request.setTermsOfServiceAgreed(true);
            ExecutorService pool = Executors.newFixedThreadPool(4);
            CountDownLatch start = new CountDownLatch(1);
            try {
                List<Future<?>> tasks = new ArrayList<>();
                for (int i = 0; i < 4; i++)
                    tasks.add(pool.submit(() -> {
                        start.await();
                        userService.agreeTerms(user.getEmail(), request);
                        return null;
                    }));
                start.countDown();
                for (var task : tasks) task.get(20, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
            }
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new UserStatisticsResult(101, 1, 0));
            var tx = new TransactionTemplate(transactions);
            tx.executeWithoutResult(status -> {
                var locked = users.findActiveByEmailForUpdate(user.getEmail()).orElseThrow();
                locked.withdraw(List.of(WithdrawalReason.NOT_USING), null, LocalDateTime.of(2026, 9, 10, 12, 0));
                recorder.recordWithdrawal(locked, LocalDateTime.of(2026, 9, 10, 12, 0));
            });
            users.deleteById(user.getId());
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new UserStatisticsResult(100, 1, 1));
        }

        @Test
        @DisplayName("같은 날짜의 동시 증가를 유실하지 않고 관리자와 미동의 회원은 제외한다")
        void itKeepsConcurrentIncrementsAndExcludesIneligibleUsers() throws Exception {
            User user = User.builder()
                    .role(Role.USER)
                    .privacyPolicyAgreed(true)
                    .termsOfServiceAgreed(true)
                    .build();
            ExecutorService pool = Executors.newFixedThreadPool(4);
            try {
                List<Future<?>> tasks = new ArrayList<>();
                for (int i = 0; i < 20; i++)
                    tasks.add(pool.submit(() -> new TransactionTemplate(transactions)
                            .executeWithoutResult(
                                    status -> recorder.recordSignup(user, LocalDateTime.of(2026, 9, 10, 12, 0)))));
                for (var task : tasks) task.get(20, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
            }
            User admin = User.builder()
                    .role(Role.ADMIN)
                    .privacyPolicyAgreed(true)
                    .termsOfServiceAgreed(true)
                    .build();
            User pending = User.builder().role(Role.USER).build();
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                recorder.recordSignup(admin, LocalDateTime.of(2026, 9, 10, 12, 0));
                recorder.recordSignup(pending, LocalDateTime.of(2026, 9, 10, 12, 0));
            });
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new UserStatisticsResult(120, 20, 0));
        }
    }

    @Nested
    @DisplayName("초기화 SQL을 실행할 때")
    class DescribeInitialize {
        @Test
        @DisplayName("도메인별 기초 수와 같은 도입 시각을 저장하고 재실행을 거부한다")
        void itInitializesBothDomainsAndRejectsReexecution() throws Exception {
            jdbc.update("DELETE FROM user_statistics");
            long expectedUsers = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE role='USER' AND deleted_at IS NULL AND privacy_policy_agreed=TRUE AND terms_of_service_agreed=TRUE",
                    Long.class);
            long expectedPosts = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM post WHERE is_deleted=FALSE AND temporary_save=FALSE", Long.class);
            String script = Files.readString(Path.of("scripts/initialize-statistics.sql"));
            String procedure = script.substring(script.indexOf("CREATE PROCEDURE"), script.indexOf("END$$") + 3);
            jdbc.execute("DROP PROCEDURE IF EXISTS initialize_statistics");
            jdbc.execute(procedure);
            try {
                jdbc.execute("CALL initialize_statistics()");
                assertThat(jdbc.queryForObject("SELECT opening_user_count FROM user_statistics", Long.class))
                        .isEqualTo(expectedUsers);
                assertThat(jdbc.queryForObject("SELECT opening_post_count FROM post_statistics", Long.class))
                        .isEqualTo(expectedPosts);
                assertThat(jdbc.queryForObject(
                                "SELECT u.started_at=p.started_at FROM user_statistics u CROSS JOIN post_statistics p",
                                Boolean.class))
                        .isTrue();
                assertThat(jdbc.queryForObject("SELECT signup_count+withdrawal_count FROM user_statistics", Long.class))
                        .isZero();
                assertThatThrownBy(() -> jdbc.execute("CALL initialize_statistics()"))
                        .isInstanceOf(DataAccessException.class);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_statistics", Long.class))
                        .isEqualTo(1);
            } finally {
                jdbc.execute("DROP PROCEDURE initialize_statistics");
            }
        }

        @Test
        @DisplayName("한쪽에 기존 행이 있으면 다른 쪽을 초기화하지 않는다")
        void itRejectsPartialInitialization() throws Exception {
            String script = Files.readString(Path.of("scripts/initialize-statistics.sql"));
            String procedure = script.substring(script.indexOf("CREATE PROCEDURE"), script.indexOf("END$$") + 3);
            jdbc.execute("DROP PROCEDURE IF EXISTS initialize_statistics");
            jdbc.execute(procedure);
            try {
                assertThatThrownBy(() -> jdbc.execute("CALL initialize_statistics()"))
                        .isInstanceOf(DataAccessException.class);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM post_statistics", Long.class))
                        .isZero();
                assertThat(jdbc.queryForObject("SELECT opening_user_count FROM user_statistics", Long.class))
                        .isEqualTo(100);
            } finally {
                jdbc.execute("DROP PROCEDURE initialize_statistics");
            }
        }

        @Test
        @DisplayName("두 번째 테이블 저장이 실패하면 첫 번째 저장도 rollback한다")
        void itRollsBackBothTablesWhenSecondInsertFails() throws Exception {
            jdbc.update("DELETE FROM user_statistics");
            String script = Files.readString(Path.of("scripts/initialize-statistics.sql"));
            String procedure = script.substring(script.indexOf("CREATE PROCEDURE"), script.indexOf("END$$") + 3);
            jdbc.execute("DROP PROCEDURE IF EXISTS initialize_statistics");
            jdbc.execute(procedure);
            jdbc.execute("ALTER TABLE post_statistics RENAME COLUMN opening_post_count TO invalid_opening_post_count");
            try {
                assertThatThrownBy(() -> jdbc.execute("CALL initialize_statistics()"))
                        .isInstanceOf(DataAccessException.class);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_statistics", Long.class))
                        .isZero();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM post_statistics", Long.class))
                        .isZero();
            } finally {
                jdbc.execute(
                        "ALTER TABLE post_statistics RENAME COLUMN invalid_opening_post_count TO opening_post_count");
                jdbc.execute("DROP PROCEDURE initialize_statistics");
            }
        }
    }

    @Nested
    @DisplayName("탈퇴와 게시글 작성이 경합할 때")
    class DescribeWithdrawal {
        @Test
        @DisplayName("탈퇴가 먼저 회원을 잠그면 새 게시글을 거부하고 회원과 공개 글을 함께 감소시킨다")
        void itRejectsCreationAfterWithdrawalAndRecordsBothCounts() throws Exception {
            jdbc.update(
                    "INSERT INTO post_statistics (statistics_date,started_at,opening_post_count) VALUES ('2026-09-08','2026-09-08 12:00:00',1)");
            User user = User.builder()
                    .email("withdraw-stats-" + UUID.randomUUID() + "@example.com")
                    .nickname("탈퇴통계" + UUID.randomUUID().toString().substring(0, 8))
                    .password("test-password")
                    .role(Role.USER)
                    .privacyPolicyAgreed(true)
                    .termsOfServiceAgreed(true)
                    .build();
            users.saveAndFlush(user);
            var post = Post.create(
                    "기존 글",
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
            posts.saveAndFlush(post);
            var principal = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                    .password("test")
                    .roles("USER")
                    .build();
            var create = new PostCreateRequest(
                    PostType.LOST,
                    "새 글",
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    "서울",
                    37.5,
                    127.0,
                    "내용",
                    Radius.DISTANCE_1000,
                    Category.ETC);
            var request = new AccountDeleteRequest();
            request.setReasons(List.of(WithdrawalReason.NOT_USING));
            var tx = new TransactionTemplate(transactions);
            ExecutorService pool = Executors.newSingleThreadExecutor();
            AtomicReference<Future<?>> creation = new AtomicReference<>();
            CountDownLatch attempted = new CountDownLatch(1);
            try {
                tx.executeWithoutResult(status -> {
                    users.findActiveByEmailForUpdate(user.getEmail()).orElseThrow();
                    creation.set(pool.submit(() -> {
                        attempted.countDown();
                        assertThatThrownBy(() -> postService.createPost(create, principal, List.of()))
                                .isInstanceOf(GeneralException.class);
                    }));
                    try {
                        assertThat(attempted.await(10, TimeUnit.SECONDS)).isTrue();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                    withdrawalService.delete(user.getEmail(), request);
                });
                creation.get().get(20, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
            }
            assertThat(users.findByEmail(user.getEmail())).isEmpty();
            assertThat(posts.findById(post.getId()).orElseThrow().isDeleted()).isTrue();
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)))
                    .isEqualTo(new UserStatisticsResult(99, 0, 1));
            assertThat(jdbc.queryForObject(
                            "SELECT SUM(opening_post_count+lost_post_count+found_post_count-removed_count) FROM post_statistics",
                            Long.class))
                    .isZero();
            assertThatThrownBy(() -> withdrawalService.delete(user.getEmail(), request))
                    .isInstanceOf(GeneralException.class);
            assertThat(service.get(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10))
                            .withdrawalCount())
                    .isEqualTo(1);
        }
    }
}
