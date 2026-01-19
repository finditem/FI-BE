package com.fmi.domain.auth.service;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.Enum.Provider;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SocialLoginService {

    private final SocialAccountsRepository socialAccountsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User upsertUserFromKakao(Long kakaoId, String email, String nickname, String profileImageUrl) {
        String providerId = String.valueOf(kakaoId);

        // 이미 존재하는 소셜 계정인지 확인
        return socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.KAKAO, providerId)
                .map(existingAccount -> {
                    log.info("기존 카카오 계정 찾음: providerId={}, userId={}", providerId, existingAccount.getUser().getId());
                    return existingAccount.getUser();
                })
                .orElseGet(() -> {
                    log.info("새 카카오 계정 생성 시작: providerId={}", providerId);
                    
                    // 이메일이 없으면 대체 이메일 생성
                    String effectiveEmail = (email != null && !email.isBlank())
                            ? email
                            : ("kakao_" + providerId + "@kakao.local");

                    // 사용자 존재 여부 확인(이메일 기준)
                    User user = userRepository.findByEmail(effectiveEmail)
                            .orElseGet(() -> {
                                log.info("새 사용자 생성: email={}", effectiveEmail);
                                User u = AuthConverter.toSocialUserEntity(
                                        kakaoId,
                                        email,
                                        nickname,
                                        profileImageUrl,
                                        passwordEncoder.encode("{noop}-" + providerId)
                                );
                                return userRepository.save(u);
                            });

                    // 동시성 문제 방지를 위해 저장 전 다시 한 번 확인
                    // (다른 스레드가 이미 저장했을 수 있음)
                    Optional<SocialAccounts> existingAccount = socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.KAKAO, providerId);
                    if (existingAccount.isPresent()) {
                        log.info("동시 요청으로 인해 이미 소셜 계정이 생성됨: providerId={}, userId={}", providerId, existingAccount.get().getUser().getId());
                        return existingAccount.get().getUser();
                    }

                    // 소셜 계정 연결 저장
                    try {
                        SocialAccounts account = SocialAccounts.builder()
                                .user(user)
                                .provider(Provider.KAKAO)
                                .providerId(providerId)
                                .build();
                        SocialAccounts savedAccount = socialAccountsRepository.save(account);
                        log.info("소셜 계정 저장 성공: socialId={}, providerId={}, userId={}", savedAccount.getSocialId(), providerId, user.getId());
                        return user;
                    } catch (DataIntegrityViolationException e) {
                        log.warn("소셜 계정 저장 중 중복 키 위반 발생 (동시 요청 가능성): providerId={}, error={}", providerId, e.getMessage());
                        // 중복 키 위반 시 다시 조회해서 반환
                        Optional<SocialAccounts> retryAccount = socialAccountsRepository.findByProviderAndProviderIdWithUser(Provider.KAKAO, providerId);
                        if (retryAccount.isPresent()) {
                            log.info("중복 키 위반 후 재조회 성공: providerId={}, userId={}", providerId, retryAccount.get().getUser().getId());
                            return retryAccount.get().getUser();
                        }
                        // 재조회도 실패한 경우
                        log.error("소셜 계정 저장 실패 후 재조회도 실패: providerId={}, 원본 에러: {}", providerId, e.getMessage(), e);
                        throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
                    }
                });
    }
}


