package com.fmi.domain.auth.service;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SocialLoginService {

    private final SocialAccountsRepository socialAccountsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NicknameGeneratorService nicknameGeneratorService;

    /** 재가입 7일 제한을 면제할 카카오 테스트 계정 이메일 목록 (쉼표 구분) */
    @Value("#{'${KAKAO_TEST_ACCOUNT_EMAILS:}'.split(',').![#this.trim()].?[!#this.isEmpty()]}")
    private Set<String> testAccountEmails;

    public record KakaoLoginResult(User user) {}

    public record AppleLoginResult(User user) {}

    public AppleLoginResult upsertUserFromApple(String subject) {
        Optional<SocialAccounts> existingAccount =
                socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.APPLE, subject);
        if (existingAccount.isPresent()) {
            User existingUser = existingAccount.get().getUser();
            log.info("기존 Apple 계정 찾음: providerId={}, userId={}", subject, existingUser.getId());
            return new AppleLoginResult(reloadUser(existingUser));
        }

        User user = User.builder()
                .email("apple_" + subject + "@apple.local")
                .password(passwordEncoder.encode("{noop}-" + subject))
                .nickname(nicknameGeneratorService.generateRandomNickname())
                .profile_img("")
                .role(Role.USER)
                .email_verified(true)
                .privacyPolicyAgreed(false)
                .termsOfServiceAgreed(false)
                .contentPolicyAgreed(false)
                .marketingConsent(false)
                .build();
        User savedUser = userRepository.save(user);

        SocialAccounts account = SocialAccounts.builder()
                .user(savedUser)
                .provider(Provider.APPLE)
                .providerId(subject)
                .build();
        socialAccountsRepository.save(account);

        log.info("새 Apple 계정 생성: providerId={}, userId={}", subject, savedUser.getId());
        return new AppleLoginResult(reloadUser(savedUser));
    }

    public KakaoLoginResult upsertUserFromKakao(Long kakaoId, String email, String nickname, String profileImageUrl) {
        String providerId = String.valueOf(kakaoId);

        // 이미 존재하는 소셜 계정인지 확인
        Optional<SocialAccounts> existingAccount =
                socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.KAKAO, providerId);
        if (existingAccount.isPresent()) {
            User existingUser = existingAccount.get().getUser();
            // 탈퇴한 사용자인 경우 재가입 방지
            if (existingUser.getDeletedAt() != null) {
                boolean isTestAccount = isTestAccount(existingUser.getEmail());
                LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
                if (!isTestAccount && existingUser.getDeletedAt().isAfter(oneWeekAgo)) {
                    log.info(
                            "최근 탈퇴한 카카오 계정 재로그인 차단: providerId={}, deletedAt={}",
                            providerId,
                            existingUser.getDeletedAt());
                    throw new GeneralException(ErrorStatus._EMAIL_RECENTLY_DELETED);
                }
                // 7일 지난 탈퇴 계정 또는 테스트 계정: User 재활성화 및 소셜 계정 재생성
                if (isTestAccount) {
                    log.info("테스트 계정 재가입 제한 면제: providerId={}, userId={}", providerId, existingUser.getId());
                }
                log.info(
                        "탈퇴 카카오 계정, User 재활성화 및 소셜 계정 재생성: providerId={}, userId={}", providerId, existingUser.getId());
                existingUser.reactivateForSocialLogin();
                userRepository.save(existingUser);
                socialAccountsRepository.delete(existingAccount.get());
                socialAccountsRepository.flush();
                return new KakaoLoginResult(reloadUser(existingUser));
            } else {
                log.info("기존 카카오 계정 찾음: providerId={}, userId={}", providerId, existingUser.getId());
                return new KakaoLoginResult(reloadUser(existingUser));
            }
        }

        log.info("새 카카오 계정 생성 시작: providerId={}", providerId);

        // 이메일이 없으면 대체 이메일 생성
        String effectiveEmail = (email != null && !email.isBlank()) ? email : ("kakao_" + providerId + "@kakao.local");

        // 이메일 기준 7일 재가입 방지 체크 (테스트 계정 제외)
        if (!isTestAccount(effectiveEmail)) {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
            if (userRepository.existsRecentlyDeletedByEmail(effectiveEmail, oneWeekAgo)) {
                log.info("최근 탈퇴한 이메일로 카카오 재가입 차단: email={}", effectiveEmail);
                throw new GeneralException(ErrorStatus._EMAIL_RECENTLY_DELETED);
            }
        }

        // 사용자 존재 여부 확인(이메일 기준)
        User user = userRepository.findByEmail(effectiveEmail).orElseGet(() -> {
            log.info("새 사용자 생성: email={}", effectiveEmail);

            // 닉네임이 없으면 랜덤 닉네임 생성
            String effectiveNickname = (nickname != null && !nickname.isBlank())
                    ? nickname
                    : nicknameGeneratorService.generateRandomNickname();

            log.info("소셜 로그인 닉네임: 원본={}, 사용={}", nickname, effectiveNickname);

            User u = AuthConverter.toSocialUserEntity(
                    kakaoId, email, effectiveNickname, profileImageUrl, passwordEncoder.encode("{noop}-" + providerId));
            return userRepository.save(u);
        });

        // 동시성 문제 방지를 위해 저장 전 다시 한 번 확인
        // (다른 스레드가 이미 저장했을 수 있음)
        Optional<SocialAccounts> existingAccountAfterUser =
                socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.KAKAO, providerId);
        if (existingAccountAfterUser.isPresent()) {
            log.info(
                    "동시 요청으로 인해 이미 소셜 계정이 생성됨: providerId={}, userId={}",
                    providerId,
                    existingAccountAfterUser.get().getUser().getId());
            return new KakaoLoginResult(
                    reloadUser(existingAccountAfterUser.get().getUser()));
        }

        // 소셜 계정 연결 저장
        try {
            SocialAccounts account = SocialAccounts.builder()
                    .user(user)
                    .provider(Provider.KAKAO)
                    .providerId(providerId)
                    .build();
            SocialAccounts savedAccount = socialAccountsRepository.save(account);
            log.info(
                    "소셜 계정 저장 성공: socialId={}, providerId={}, userId={}",
                    savedAccount.getSocialId(),
                    providerId,
                    user.getId());
            return new KakaoLoginResult(reloadUser(user));
        } catch (DataIntegrityViolationException e) {
            log.warn("소셜 계정 저장 중 중복 키 위반 발생 (동시 요청 가능성): providerId={}, error={}", providerId, e.getMessage());
            // 중복 키 위반 시 다시 조회해서 반환
            Optional<SocialAccounts> retryAccount =
                    socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.KAKAO, providerId);
            if (retryAccount.isPresent()) {
                log.info(
                        "중복 키 위반 후 재조회 성공: providerId={}, userId={}",
                        providerId,
                        retryAccount.get().getUser().getId());
                return new KakaoLoginResult(reloadUser(retryAccount.get().getUser()));
            }
            // 재조회도 실패한 경우
            log.error("소셜 계정 저장 실패 후 재조회도 실패: providerId={}, 원본 에러: {}", providerId, e.getMessage(), e);
            throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isTestAccount(String email) {
        return testAccountEmails != null && testAccountEmails.contains(email);
    }

    private User reloadUser(User user) {
        return userRepository
                .findById(user.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
    }
}
