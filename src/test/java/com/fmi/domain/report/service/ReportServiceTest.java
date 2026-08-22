package com.fmi.domain.report.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.report.converter.ReportConverter;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.data.enums.ReportType;
import com.fmi.domain.report.repository.ReportAnswerImageRepository;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.report.web.dto.request.ReportCreateRequestDTO;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.userblock.service.BlockService;
import com.fmi.service.EmailService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ReportConverter reportConverter;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private ReportAnswerImageRepository reportAnswerImageRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BlockService blockService;

    @InjectMocks
    private ReportService reportService;

    private User reporter;
    private User targetUser;
    private ChatRoom chatRoom;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        reporter = User.builder()
                .id(1L)
                .email("reporter@test.com")
                .nickname("신고자")
                .role(Role.USER)
                .build();

        targetUser = User.builder()
                .id(2L)
                .email("target@test.com")
                .nickname("피신고자")
                .role(Role.USER)
                .build();

        chatRoom = mock(ChatRoom.class);
        given(chatRoom.getId()).willReturn(10L);
        given(chatRoom.isParticipant(reporter)).willReturn(true);
        given(chatRoom.getOtherParticipant(reporter.getId())).willReturn(targetUser);

        userDetails = mock(UserDetails.class);
        given(userDetails.getUsername()).willReturn(reporter.getEmail());
    }

    @Test
    @DisplayName("채팅 신고 시 상대방이 자동으로 차단된다")
    void createReport_chat_autoBlocksOpponent() {
        // given
        ReportCreateRequestDTO request =
                new ReportCreateRequestDTO(ReportTargetType.CHAT, 10L, ReportType.SPAM, "스팸 메시지");

        given(userRepository.findByEmail(reporter.getEmail())).willReturn(Optional.of(reporter));
        given(reportRepository.findByReporterAndTargetTypeAndTargetId(reporter, ReportTargetType.CHAT, 10L))
                .willReturn(Optional.empty());
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));

        Report savedReport = Report.builder()
                .reportId(100L)
                .reporter(reporter)
                .targetType(ReportTargetType.CHAT)
                .targetId(10L)
                .reportType(ReportType.SPAM)
                .build();
        given(reportRepository.save(any(Report.class))).willReturn(savedReport);

        // when
        reportService.createReport(request, userDetails);

        // then
        then(blockService).should().block(reporter.getId(), targetUser.getId());
    }

    @Test
    @DisplayName("채팅 외 신고(게시글)는 자동 차단이 발생하지 않는다")
    void createReport_nonChat_doesNotBlock() {
        // given
        ReportCreateRequestDTO request =
                new ReportCreateRequestDTO(ReportTargetType.POST, 99L, ReportType.SPAM, "스팸 게시글");

        given(userRepository.findByEmail(reporter.getEmail())).willReturn(Optional.of(reporter));
        given(reportRepository.findByReporterAndTargetTypeAndTargetId(reporter, ReportTargetType.POST, 99L))
                .willReturn(Optional.empty());
        given(postRepository.findById(99L)).willReturn(Optional.of(mock(com.fmi.domain.post.data.Post.class)));

        Report savedReport = Report.builder()
                .reportId(101L)
                .reporter(reporter)
                .targetType(ReportTargetType.POST)
                .targetId(99L)
                .reportType(ReportType.SPAM)
                .build();
        given(reportRepository.save(any(Report.class))).willReturn(savedReport);

        // when
        reportService.createReport(request, userDetails);

        // then
        then(blockService).shouldHaveNoInteractions();
    }
}
