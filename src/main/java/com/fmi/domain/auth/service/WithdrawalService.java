package com.fmi.domain.auth.service;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.Enum.WithdrawalReason;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.auth.web.dto.AccountDeleteRequest;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.external.oauth.kakao.KakaoOAuthClient;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import com.fmi.security.RefreshTokenStore;
import com.fmi.service.EmailService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawalService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final EmailService emailService;
    private final RefreshTokenStore refreshTokenStore;
    private final PostService postService;
    private final SocialAccountsRepository socialAccountsRepository;
    private final KakaoOAuthClient kakaoOAuthClient;

    public void delete(String email, AccountDeleteRequest request) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        deleteProfileImage(user);
        validateOtherReason(request);
        socialAccountsRepository
                .findByUser(user)
                .filter(account -> account.getProvider() == Provider.KAKAO)
                .ifPresent(account -> kakaoOAuthClient.unlinkUser(account.getProviderId()));

        user.withdraw(request.getReasons(), request.getOtherReason(), LocalDateTime.now());
        userRepository.save(user);
        refreshTokenStore.revokeAllForUser(email);
        postService.softDeleteAllByUser(user);
        sendDeletionEmail(user);
        log.info("사용자 탈퇴 완료: userId={}, email={}, reasons={}", user.getId(), email, request.getReasons());
    }

    private void deleteProfileImage(User user) {
        if (user.getProfile_img() == null || user.getProfile_img().isEmpty()) {
            return;
        }
        if (!s3Service.isValidS3Url(user.getProfile_img())) {
            log.warn("유효하지 않은 프로필 이미지 URL, S3 삭제 생략: {}", user.getProfile_img());
            return;
        }
        try {
            s3Service.delete(List.of(user.getProfile_img()));
        } catch (Exception e) {
            log.warn("프로필 이미지 삭제 실패: {}", e.getMessage());
        }
    }

    private void validateOtherReason(AccountDeleteRequest request) {
        if (request.getReasons().contains(WithdrawalReason.OTHER)
                && (request.getOtherReason() == null || request.getOtherReason().isBlank())) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
    }

    private void sendDeletionEmail(User user) {
        try {
            String nickname = user.getNickname() != null ? user.getNickname() : "회원";
            String deletionDate = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일").format(LocalDateTime.now());
            emailService.sendHtmlEmailAsync(
                    user.getEmail(),
                    "계정이 삭제되었습니다",
                    "account-deletion-email.html",
                    Map.of("NAME", nickname, "USER", user.getEmail(), "DATE", deletionDate));
        } catch (Exception e) {
            log.warn("계정 삭제 이메일 발송 실패: {}", e.getMessage());
        }
    }
}
