package com.fmi.global.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fmi.domain.auth.security.jwt.JwtTokenProvider;
import com.fmi.domain.chat.message.data.*;
import com.fmi.domain.chat.message.data.enums.MessageType;
import com.fmi.domain.chat.message.repository.ChatMessageRepository;
import com.fmi.domain.chat.message.web.dto.request.ChatMessageRequest.SendMessageRequest;
import com.fmi.domain.chat.room.data.*;
import com.fmi.domain.chat.room.repository.ChatRoomRepository;
import com.fmi.domain.inquiry.comment.data.InquiryComment;
import com.fmi.domain.inquiry.comment.data.InquiryCommentImage;
import com.fmi.domain.inquiry.comment.repository.InquiryCommentImageRepository;
import com.fmi.domain.inquiry.comment.repository.InquiryCommentRepository;
import com.fmi.domain.inquiry.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.inquiry.repository.InquiryRepository;
import com.fmi.domain.notice.comment.data.*;
import com.fmi.domain.notice.comment.repository.*;
import com.fmi.domain.notice.notice.data.Notice;
import com.fmi.domain.notice.notice.repository.NoticeRepository;
import com.fmi.domain.place.data.*;
import com.fmi.domain.place.data.enums.*;
import com.fmi.domain.place.repository.PlaceRepository;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.*;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.user.temperature.data.enums.TemperatureActivityType;
import com.fmi.domain.user.temperature.service.TemperatureService;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.external.mail.EmailSender;
import com.fmi.external.storage.S3Service;
import com.fmi.support.IntegrationTestSupport;
import com.fmi.support.SqlCaptureConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Type;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(SqlCaptureConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.jpa.properties.hibernate.generate_statistics=true",
            "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true"
        })
@DisplayName("JPA 연관관계 외부 진입점 회귀 검증")
class JpaRelationshipRegressionTest extends IntegrationTestSupport {
    @Autowired
    MockMvc mvc;

    @Autowired
    TestRestTemplate http;

    @LocalServerPort
    int port;

    @Autowired
    JwtTokenProvider tokens;

    @MockitoBean
    EmailSender emailSender;

    @Autowired
    S3Service storage;

    @Autowired
    ReportRepository reports;

    @Autowired
    UserRepository users;

    @Autowired
    PlaceRepository places;

    @Autowired
    NoticeRepository notices;

    @Autowired
    NoticeCommentRepository noticeComments;

    @Autowired
    NoticeCommentImageRepository noticeImages;

    @Autowired
    InquiryRepository inquiries;

    @Autowired
    InquiryCommentRepository inquiryComments;

    @Autowired
    InquiryCommentImageRepository inquiryImages;

    @Autowired
    ChatRoomRepository rooms;

    @Autowired
    ChatMessageRepository messages;

    @Autowired
    TemperatureService temperatures;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Nested
    @DisplayName("장소 즐겨찾기를 변경하고 조회할 때")
    class DescribeFavorites {
        @Test
        @DisplayName("반복 등록과 취소를 HTTP로 처리하고 커밋한 저장 상태를 반환한다")
        void itCommitsFavoriteAndCancellation() throws Exception {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User owner = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            Place place = Place.builder()
                    .name("회귀 카페")
                    .address("서울")
                    .latitude(37.5)
                    .longitude(127)
                    .station("성수")
                    .stationDistanceMeters(100)
                    .type(PlaceType.CAFE)
                    .thumbnailUrl("test")
                    .build();
            for (DayOfWeek day : DayOfWeek.values()) {
                place.addBusinessHour(PlaceBusinessHour.builder()
                        .dayOfWeek(day)
                        .closed(false)
                        .timeRange(new PlaceTimeRange(
                                PlaceBusinessHourType.BUSINESS, LocalTime.of(9, 0), LocalTime.of(22, 0)))
                        .build());
            }
            places.save(place);
            // when
            mvc.perform(post("/places/{id}/favorites", place.getId()).with(user(email)))
                    .andExpect(status().isOk());
            mvc.perform(post("/places/{id}/favorites", place.getId()).with(user(email)))
                    .andExpect(status().isOk());
            // then
            assertThat(jdbc.queryForObject(
                            "select count(*) from place_favorite where user_id=? and place_id=?",
                            Integer.class,
                            owner.getId(),
                            place.getId()))
                    .isEqualTo(1);
            mvc.perform(get("/users/me/favorite-places").with(user(email)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.totalCount").value(1));
            mvc.perform(delete("/places/{id}/favorites", place.getId()).with(user(email)))
                    .andExpect(status().isOk());
            assertThat(jdbc.queryForObject(
                            "select is_favorite from place_favorite where user_id=? and place_id=?",
                            Boolean.class,
                            owner.getId(),
                            place.getId()))
                    .isFalse();
        }

        @ParameterizedTest
        @ValueSource(ints = {10, 100})
        @DisplayName("후보가 늘어나도 같은 페이지 크기의 SQL 건수가 증가하지 않는다")
        void itMeasuresFavoriteQueries(int count) throws Exception {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User owner = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.executeWithoutResult(status -> {
                for (int i = 0; i < count; i++) {
                    Place place = Place.builder()
                            .name("측정 카페" + i)
                            .address("서울")
                            .latitude(37.5)
                            .longitude(127)
                            .station("성수")
                            .stationDistanceMeters(100)
                            .type(PlaceType.CAFE)
                            .thumbnailUrl("test")
                            .build();
                    for (DayOfWeek day : DayOfWeek.values()) {
                        place.addBusinessHour(PlaceBusinessHour.builder()
                                .dayOfWeek(day)
                                .closed(false)
                                .timeRange(new PlaceTimeRange(
                                        PlaceBusinessHourType.BUSINESS, LocalTime.of(9, 0), LocalTime.of(22, 0)))
                                .build());
                    }
                    places.saveAndFlush(place);
                    jdbc.update(
                            "insert into place_favorite (user_id,place_id,is_favorite,entity_status,created_at,updated_at) values (?,?,true,'ACTIVE',?,?)",
                            owner.getId(),
                            place.getId(),
                            LocalDateTime.of(2026, 9, 10, 11, 0),
                            LocalDateTime.of(2026, 9, 10, 11, 0).plusSeconds(i));
                }
            });
            var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            mvc.perform(get("/users/me/favorite-places").param("size", "10").with(user(email)))
                    .andExpect(status().isOk());
            // when & then: 준비 쿼리는 제외하고 같은 HTTP 요청을 5회 측정한다.
            for (int sample = 1; sample <= 5; sample++) {
                statistics.clear();
                SqlCaptureConfiguration.reset();
                long start = System.nanoTime();
                mvc.perform(get("/users/me/favorite-places").param("size", "10").with(user(email)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.result.totalCount").value(count));
                double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
                long statements = statistics.getPrepareStatementCount();
                List<String> executed = SqlCaptureConfiguration.statements();
                System.out.printf(
                        "JPA_MEASUREMENT favorites candidates=%d pageSize=10 sample=%d hibernateStatements=%d totalStatements=%d entities=%d elapsedMs=%.3f%n",
                        count, sample, statements, executed.size(), statistics.getEntityLoadCount(), elapsedMs);
                if (sample == 1) {
                    executed.forEach(sql -> System.out.println("CAPTURED_SQL " + sql.replaceAll("\\s+", " ")));
                }
                assertThat(executed).hasSize(4);
                assertThat(statements).isEqualTo(3);
            }
            List<String> executionPlan = jdbc.queryForList("""
                EXPLAIN ANALYZE SELECT pf.place_id, pf.updated_at
                FROM place_favorite pf JOIN place p ON p.id = pf.place_id
                WHERE pf.user_id = ? AND pf.is_favorite = TRUE
                  AND pf.entity_status = 'ACTIVE' AND p.entity_status = 'ACTIVE'
                ORDER BY pf.updated_at DESC
                """, String.class, owner.getId());
            executionPlan.forEach(plan -> System.out.println("EXPLAIN_FAVORITES candidates=" + count + " " + plan));
        }
    }

    @Nested
    @DisplayName("공지 댓글을 삭제할 때")
    class DescribeNoticeDeletion {
        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("이미지 유무와 관계없이 답글이 있는 부모는 내용만 삭제하고 답글과 부모 관계를 보존한다")
        void itKeepsReplyAndSoftDeletesParent(boolean withImage) throws Exception {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User owner = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            Notice notice = notices.save(
                    Notice.builder().title("공지").content("본문").author(owner).build());
            NoticeComment parent = noticeComments.save(NoticeComment.builder()
                    .notice(notice)
                    .user(owner)
                    .content("부모")
                    .build());
            NoticeComment reply = noticeComments.save(NoticeComment.builder()
                    .notice(notice)
                    .user(owner)
                    .parent(parent)
                    .content("답글")
                    .build());
            String url = "https://test.invalid/notice-" + parent.getId();
            if (withImage) {
                noticeImages.save(NoticeCommentImage.create(url, parent));
            }
            // when
            mvc.perform(delete("/notices/comments/{id}", parent.getId()).with(user(email)))
                    .andExpect(status().isOk());
            // then
            assertThat(jdbc.queryForObject(
                            "select deleted from notice_comment where comment_id=?", Boolean.class, parent.getId()))
                    .isTrue();
            assertThat(jdbc.queryForObject(
                            "select parent_id from notice_comment where comment_id=?", Long.class, reply.getId()))
                    .isEqualTo(parent.getId());
            assertThat(noticeImages.findByComment_Id(parent.getId())).isEmpty();
            if (withImage) {
                verify(storage).delete(List.of(url));
            }
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("이미지 유무와 관계없이 본인 말단 댓글은 삭제하고 다른 사용자의 삭제는 거절한다")
        void itDeletesLeafAndRejectsOtherUser(boolean withImage) throws Exception {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User owner = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            Notice notice = notices.save(
                    Notice.builder().title("공지").content("본문").author(owner).build());
            NoticeComment comment = noticeComments.save(NoticeComment.builder()
                    .notice(notice)
                    .user(owner)
                    .content("댓글")
                    .build());
            if (withImage) {
                noticeImages.save(NoticeCommentImage.create("https://test.invalid/leaf-" + comment.getId(), comment));
            }
            // when
            mvc.perform(delete("/notices/comments/{id}", comment.getId()).with(user("other@test.com")))
                    .andExpect(status().isForbidden());
            // then
            assertThat(noticeComments.existsById(comment.getId())).isTrue();
            mvc.perform(delete("/notices/comments/{id}", comment.getId()).with(user(email)))
                    .andExpect(status().isOk());
            assertThat(noticeComments.existsById(comment.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("문의 댓글을 삭제할 때")
    class DescribeInquiryDeletion {
        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("부모와 자손 댓글 및 이미지를 삭제하고 같은 문의의 다른 댓글은 보존한다")
        void itDeletesSubtreeOnly(boolean withImages) throws Exception {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User owner = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            Inquiry inquiry = inquiries.save(Inquiry.builder()
                    .user(owner)
                    .title("문의")
                    .content("본문")
                    .inquiryType(InquiryType.ETC)
                    .build());
            InquiryComment parent = inquiryComments.save(InquiryComment.builder()
                    .inquiry(inquiry)
                    .user(owner)
                    .content("부모")
                    .build());
            InquiryComment child = inquiryComments.save(InquiryComment.builder()
                    .inquiry(inquiry)
                    .user(owner)
                    .parent(parent)
                    .content("답글")
                    .build());
            InquiryComment grandchild = inquiryComments.save(InquiryComment.builder()
                    .inquiry(inquiry)
                    .user(owner)
                    .parent(child)
                    .content("자손")
                    .build());
            InquiryComment sibling = inquiryComments.save(InquiryComment.builder()
                    .inquiry(inquiry)
                    .user(owner)
                    .content("다른 댓글")
                    .build());
            String rootUrl = "https://test.invalid/inquiry-root-" + parent.getId();
            String childUrl = "https://test.invalid/inquiry-child-" + child.getId();
            if (withImages) {
                inquiryImages.save(InquiryCommentImage.create(rootUrl, parent));
                inquiryImages.save(InquiryCommentImage.create(childUrl, child));
            }
            // when
            mvc.perform(delete("/inquiries/{inquiryId}/comments/{id}", inquiry.getId(), parent.getId())
                            .with(user(email)))
                    .andExpect(status().isOk());
            // then
            assertThat(inquiryComments.existsById(parent.getId())).isFalse();
            assertThat(inquiryComments.existsById(child.getId())).isFalse();
            assertThat(inquiryComments.existsById(grandchild.getId())).isFalse();
            assertThat(inquiryComments.existsById(sibling.getId())).isTrue();
            assertThat(inquiryImages.findByComment_IdIn(List.of(parent.getId(), child.getId(), grandchild.getId())))
                    .isEmpty();
            if (withImages) {
                verify(storage)
                        .delete(argThat(urls -> urls.size() == 2 && urls.containsAll(List.of(rootUrl, childUrl))));
            }
        }
    }

    @Nested
    @DisplayName("온도 활동을 기록하고 재계산할 때")
    class DescribeTemperature {
        @Test
        @DisplayName("원장과 공유 기본키 집계를 커밋하고 HTTP로 계산 결과를 반환한다")
        void itPersistsAndReturnsTemperature() throws Exception {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User owner = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            // when
            temperatures.record(
                    TemperatureActivityType.RETURN_COMPLETED,
                    owner.getId(),
                    owner.getId(),
                    LocalDateTime.of(2026, 9, 9, 12, 0));
            temperatures.recalculate();
            // then
            assertThat(jdbc.queryForObject(
                            "select temperature_tenths from user_temperature where user_id=?",
                            Integer.class,
                            owner.getId()))
                    .isGreaterThan(365);
            mvc.perform(get("/users/me/temperature").with(user(email))).andExpect(status().isOk());
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            long sourceId = -owner.getId();
            tx.executeWithoutResult(status -> {
                temperatures.record(
                        TemperatureActivityType.RETURN_COMPLETED,
                        owner.getId(),
                        sourceId,
                        LocalDateTime.of(2026, 9, 9, 12, 0));
                entityManager.flush();
                status.setRollbackOnly();
            });
            assertThat(jdbc.queryForObject(
                            "select count(*) from temperature_activity where source_id=?", Integer.class, sourceId))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("채팅방과 이미지 메시지를 저장하고 조회할 때")
    class DescribeChat {
        @Test
        @DisplayName("참여자와 이미지의 cascade 저장을 커밋하고 HTTP로 이미지를 조회한다")
        void itPersistsParticipantsAndImages() throws Exception {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User owner = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            User other = users.save(User.builder()
                    .email("other-" + email)
                    .nickname("other-" + email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            ChatRoom room = ChatRoom.builder()
                    .createdAt(LocalDateTime.of(2026, 9, 10, 12, 0))
                    .build();
            room.addParticipant(ChatRoomParticipant.builder().user(owner).build());
            room.addParticipant(ChatRoomParticipant.builder().user(other).build());
            rooms.save(room);
            ChatMessage message = ChatMessage.builder()
                    .chatRoom(room)
                    .user(owner)
                    .messageType(MessageType.IMAGE)
                    .createdAt(LocalDateTime.of(2026, 9, 10, 12, 0))
                    .build();
            message.addImage(MessageImage.builder()
                    .imageUrl("https://test.invalid/image.png")
                    .build());
            when(storage.upload(anyList())).thenReturn(List.of("https://test.invalid/upload.png"));
            // when
            messages.save(message);
            mvc.perform(multipart("/chats/{id}/images", room.getId())
                            .file(new MockMultipartFile("images", "image.png", "image/png", new byte[] {1}))
                            .with(user(email)))
                    .andExpect(status().isOk());
            // then
            assertThat(jdbc.queryForObject(
                            "select count(*) from chat_room_participant where chat_room_id=?",
                            Integer.class,
                            room.getId()))
                    .isEqualTo(2);
            assertThat(jdbc.queryForObject(
                            "select count(*) from message_image where chat_message_id=?",
                            Integer.class,
                            message.getId()))
                    .isEqualTo(1);
            mvc.perform(get("/chats/{id}/messages", room.getId()).with(user(email)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("https://test.invalid/image.png")));
        }
    }

    @Nested
    @DisplayName("신고 답변 이미지를 교체할 때")
    class DescribeReportImages {
        @Test
        @DisplayName("HTTP 답변 수정 후 새 이미지만 저장하고 알림을 커밋한다")
        void itReplacesImagesAndCommitsNotification() throws Exception {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User reporter = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            String adminEmail = "admin-" + email;
            User admin = users.save(User.builder()
                    .email(adminEmail)
                    .nickname(adminEmail)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.ADMIN)
                    .build());
            Report report = reports.save(Report.builder()
                    .reporter(reporter)
                    .targetType(ReportTargetType.USER)
                    .targetId(reporter.getId())
                    .reportType(ReportType.ETC)
                    .reason("확인 요청")
                    .build());
            // when
            mvc.perform(put("/admin/reports/{id}/answer", report.getReportId())
                            .with(user(adminEmail).roles("ADMIN"))
                            .contentType("application/json")
                            .content("{\"adminAnswer\":\"확인했습니다\",\"images\":[\"https://test.invalid/old.png\"]}"))
                    .andExpect(status().isOk());
            mvc.perform(put("/admin/reports/{id}/answer", report.getReportId())
                            .with(user(adminEmail).roles("ADMIN"))
                            .contentType("application/json")
                            .content("{\"adminAnswer\":\"조치했습니다\",\"images\":[\"https://test.invalid/new.png\"]}"))
                    .andExpect(status().isOk());
            // then
            assertThat(jdbc.queryForList(
                            "select image_url from report_answer_image where report_id=?",
                            String.class,
                            report.getReportId()))
                    .containsExactly("https://test.invalid/new.png");
            assertThat(jdbc.queryForObject(
                            "select count(*) from notification where user_id=? and reference_id=?",
                            Integer.class,
                            reporter.getId(),
                            report.getReportId()))
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("실제 HTTP 서버에서 인증된 온도를 조회할 때")
    class DescribeRunningServer {
        @Test
        @DisplayName("쿠키 인증 후 실제 WebSocket STOMP로 메시지를 전송하고 DB에 커밋한다")
        void itSendsMessageThroughWebSocket() throws Exception {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User owner = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            User other = users.save(User.builder()
                    .email("other-" + email)
                    .nickname("other-" + email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            ChatRoom room = ChatRoom.builder()
                    .createdAt(LocalDateTime.of(2026, 9, 10, 12, 0))
                    .build();
            room.addParticipant(ChatRoomParticipant.builder().user(owner).build());
            room.addParticipant(ChatRoomParticipant.builder().user(other).build());
            rooms.save(room);
            WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
            client.setMessageConverter(new MappingJackson2MessageConverter());
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add(
                    "Cookie",
                    "access_token="
                            + tokens.createAccessToken(email, Map.of("userId", owner.getId(), "role", "ROLE_USER")));
            CompletableFuture<Map<?, ?>> received = new CompletableFuture<>();
            StompSession session = null;
            try {
                session = client.connectAsync(
                                "ws://localhost:" + port + "/ws",
                                headers,
                                new StompHeaders(),
                                new StompSessionHandlerAdapter() {})
                        .get(10, TimeUnit.SECONDS);
                session.subscribe("/user/queue/messages", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders stompHeaders) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders stompHeaders, Object payload) {
                        received.complete((Map<?, ?>) payload);
                    }
                });
                // when
                session.send("/app/chats/" + room.getId() + "/send", new SendMessageRequest("실제 STOMP 검증"));
                Map<?, ?> response = received.get(10, TimeUnit.SECONDS);
                // then
                assertThat(response.get("content")).isEqualTo("실제 STOMP 검증");
                await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(jdbc.queryForObject(
                                "select count(*) from chat_message where chat_room_id=? and content=?",
                                Integer.class,
                                room.getId(),
                                "실제 STOMP 검증"))
                        .isEqualTo(1));
            } finally {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
                client.stop();
            }
        }

        @Test
        @DisplayName("JWT 인증과 실제 네트워크 요청으로 저장된 온도를 반환한다")
        void itReturnsTemperatureThroughNetwork() {
            // given
            String email = UUID.randomUUID() + "@test.com";
            User owner = users.save(User.builder()
                    .email(email)
                    .nickname(email)
                    .password("test")
                    .profile_img("https://test.invalid/profile.png")
                    .role(Role.USER)
                    .build());
            temperatures.record(
                    TemperatureActivityType.RETURN_COMPLETED,
                    owner.getId(),
                    owner.getId(),
                    LocalDateTime.of(2026, 9, 9, 12, 0));
            temperatures.recalculate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tokens.createAccessToken(email, Map.of()));
            // when
            var response =
                    http.exchange("/users/me/temperature", HttpMethod.GET, new HttpEntity<>(headers), String.class);
            // then
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("38.5");
        }
    }
}
