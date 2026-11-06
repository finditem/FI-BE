package com.fmi.domain.user.user.service;

import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.user.user.converter.UserConverter;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.domain.user.user.service.internal.NicknameValidator;
import com.fmi.domain.user.user.web.dto.request.UserUpdateRequest;
import com.fmi.domain.user.user.web.dto.response.UserProfileResponse;
import com.fmi.external.storage.S3Service;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    private final S3Service s3Service;

    private final SocialAccountsRepository socialAccountsRepository;

    private final NicknameValidator nicknameValidator;

    private final UserStatisticsRecorder userStatisticsRecorder;

    private final Clock clock;

    /**
     * 내 정보 조회
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        boolean isSocialUser = socialAccountsRepository.existsByUser(user);
        return UserConverter.toUserProfileResponse(user, isSocialUser);
    }

    /**
     * 약관 동의 처리
     */
    @Transactional
    public void agreeTerms(String email, com.fmi.domain.user.user.web.dto.request.TermsAgreeRequest request) {
        User user = userRepository
                .findActiveByEmailForUpdate(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        boolean wasMember = user.isStatisticsMember();
        user.agreeTerms(
                request.isPrivacyPolicyAgreed(),
                request.isTermsOfServiceAgreed(),
                request.isContentPolicyAgreed(),
                request.isMarketingConsent());
        userRepository.save(user);
        if (!wasMember && user.isStatisticsMember()) {
            LocalDateTime occurredAt = LocalDateTime.now(clock);
            userStatisticsRecorder.recordSignup(user, occurredAt);
        }
    }

    /**
     * 내 정보 수정 (닉네임 + 프로필 이미지 통합, multipart/form-data)
     */
    public UserProfileResponse updateMyProfile(
            String email, UserUpdateRequest request, MultipartFile profileImage, boolean deleteProfileImage) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        LocalDateTime updatedAt = LocalDateTime.now();

        // 닉네임 유효성 검사 및 중복 체크 (request가 존재하고 nickname 필드가 명시적으로 전송된 경우만)
        if (request != null && request.isNicknameProvided() && request.getNickname() != null) {
            if (request.getNickname().equals(user.getNickname())) {
                nicknameValidator.validate(request.getNickname());
            } else {
                nicknameValidator.validateAvailable(request.getNickname());
            }
            user.changeNickname(request.getNickname(), updatedAt);
        }

        // 기존 프로필 이미지 S3 삭제 (새 이미지 업로드 또는 삭제 요청 시)
        boolean shouldDeleteOldImage = (profileImage != null && !profileImage.isEmpty()) || deleteProfileImage;
        if (shouldDeleteOldImage
                && user.getProfile_img() != null
                && !user.getProfile_img().isEmpty()) {
            if (s3Service.isValidS3Url(user.getProfile_img())) {
                try {
                    s3Service.delete(List.of(user.getProfile_img()));
                } catch (Exception e) {
                    log.warn("기존 프로필 이미지 삭제 실패: {}", e.getMessage());
                }
            } else {
                log.warn("유효하지 않은 프로필 이미지 URL, S3 삭제 생략: {}", user.getProfile_img());
            }
        }

        // 프로필 이미지 처리
        if (profileImage != null && !profileImage.isEmpty()) {
            List<String> uploadedUrls = s3Service.upload(List.of(profileImage));
            user.changeProfileImage(uploadedUrls.get(0), updatedAt);
        } else if (deleteProfileImage) {
            user.removeProfileImage(updatedAt);
        }

        user.recordProfileUpdate(updatedAt);
        User updatedUser = userRepository.save(user);
        boolean isSocialUser = socialAccountsRepository.findByUser(updatedUser).isPresent();
        return UserConverter.toUserProfileResponse(updatedUser, isSocialUser);
    }
}
