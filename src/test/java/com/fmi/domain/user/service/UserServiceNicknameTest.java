package com.fmi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.comment.service.CommentImageService;
import com.fmi.domain.commentlike.service.CommentLikeService;
import com.fmi.domain.inquiry.repository.InquiryRepository;
import com.fmi.domain.inquirycomment.repository.InquiryCommentRepository;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.user.service.internal.NicknameValidator;
import com.fmi.domain.user.web.dto.UserUpdateRequest;
import com.fmi.domain.userblock.repository.BlockedUserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import com.fmi.service.UserQueryService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 닉네임 변경")
class UserServiceNicknameTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostFavoriteRepository postFavoriteRepository;

    @Mock
    private PostQueryService postQueryService;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CommentLikeService commentLikeService;

    @Mock
    private CommentImageService commentImageService;

    @Mock
    private InquiryCommentRepository inquiryCommentRepository;

    @Mock
    private SocialAccountsRepository socialAccountsRepository;

    @Mock
    private NicknameValidator nicknameValidator;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("프로필 닉네임을 변경할 때")
    class DescribeUpdateNickname {

        @Test
        @DisplayName("닉네임 정책을 위반하면 닉네임을 변경하지 않는다")
        void itDoesNotChangeInvalidNickname() {
            User user =
                    User.builder().email("member@finditem.kr").nickname("기존닉네임").build();
            UserUpdateRequest request = new UserUpdateRequest();
            request.setNickname("가나다라마바사아자차카");
            when(userRepository.findByEmail("member@finditem.kr")).thenReturn(Optional.of(user));
            when(nicknameValidator.validate(request.getNickname()))
                    .thenReturn(NicknameValidator.ValidationResult.invalid("invalid"));

            assertThatThrownBy(() -> userService.updateMyProfile("member@finditem.kr", request, null, false))
                    .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                            .isEqualTo(ErrorStatus._INVALID_NICKNAME));
            assertThat(user.getNickname()).isEqualTo("기존닉네임");
            verify(userRepository, never()).save(user);
        }

        @Test
        @DisplayName("정책과 중복 검사를 통과하면 닉네임을 변경한다")
        void itChangesAvailableNickname() {
            User user =
                    User.builder().email("member@finditem.kr").nickname("기존닉네임").build();
            UserUpdateRequest request = new UserUpdateRequest();
            request.setNickname("새닉네임");
            when(userRepository.findByEmail("member@finditem.kr")).thenReturn(Optional.of(user));
            when(nicknameValidator.validate("새닉네임")).thenReturn(NicknameValidator.ValidationResult.success());
            when(nicknameValidator.validateAvailable("새닉네임")).thenReturn(NicknameValidator.ValidationResult.success());
            when(userRepository.save(user)).thenReturn(user);
            when(socialAccountsRepository.findByUser(user)).thenReturn(Optional.empty());

            userService.updateMyProfile("member@finditem.kr", request, null, false);

            assertThat(user.getNickname()).isEqualTo("새닉네임");
            verify(userRepository).save(user);
        }
    }
}
