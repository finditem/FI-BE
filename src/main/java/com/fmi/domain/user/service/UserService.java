package com.fmi.domain.user.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.post.converter.PostConverter;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.response.PostListResponse;
import com.fmi.domain.user.converter.UserConverter;
import com.fmi.domain.user.response.ImageUploadResponse;
import com.fmi.domain.user.response.UserCommentSummaryResponse;
import com.fmi.domain.user.response.UserOtherPageResponse;
import com.fmi.domain.user.response.UserProfileResponse;
import com.fmi.domain.user.web.dto.AccountDeleteRequest;
import com.fmi.domain.user.web.dto.PasswordChangeRequest;
import com.fmi.domain.user.web.dto.ProfileImageUpdateRequest;
import com.fmi.domain.user.web.dto.UserUpdateRequest;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final PostRepository postRepository;
    private final PostConverter postConverter;
    private final CommentRepository commentRepository;

    /**
     * 내 정보 조회
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        
        return UserConverter.toUserProfileResponse(user);
    }

    /**
     * 타인 페이지 조회
     */
    @Transactional(readOnly = true)
    public UserOtherPageResponse getOtherUserPage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        List<PostListResponse> posts = postRepository.findAllPublishedWithImagesByUser(user).stream()
                .map(postConverter::toPostListResponse)
                .toList();

        List<UserCommentSummaryResponse> comments = commentRepository.findAllWithPostByUser(user).stream()
                .map(UserConverter::toUserCommentSummaryResponse)
                .toList();

        return UserConverter.toUserOtherPageResponse(user, posts, comments);
    }

    /**
     * 내 정보 수정 (닉네임만 수정 가능)
     */
    public UserProfileResponse updateMyProfile(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 닉네임 중복 체크 (본인의 닉네임이 아닌 경우만)
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new GeneralException(ErrorStatus._NICKNAME_DUPLICATED);
            }
        }

        // User 닉네임 업데이트
        UserConverter.updateUserFromRequest(user, request);
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        return UserConverter.toUserProfileResponse(updatedUser);
    }

    /**
     * 프로필 이미지 업데이트 및 삭제
     */
    public UserProfileResponse updateProfileImage(String email, ProfileImageUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 기존 이미지 삭제 (S3에서)
        if (user.getProfile_img() != null && !user.getProfile_img().isEmpty()) {
            try {
                s3Service.delete(List.of(user.getProfile_img()));
            } catch (Exception e) {
                log.warn("기존 프로필 이미지 삭제 실패: {}", e.getMessage());
            }
        }

        // 새 이미지 설정 (null이면 삭제, 값이 있으면 업데이트)
        user.setProfile_img(request.getProfileImageUrl());
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        return UserConverter.toUserProfileResponse(updatedUser);
    }

    /**
     * 현재 비밀번호 검증
     */
    public boolean verifyPassword(String email, String currentPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        boolean passwordMatches = false;
        
        // 임시 비밀번호가 활성화되어 있는지 확인
        boolean hasActiveTemporaryPassword = user.getTemporaryPassword() != null 
                && user.getTemporaryPasswordExpiresAt() != null
                && LocalDateTime.now().isBefore(user.getTemporaryPasswordExpiresAt());
        
        // 임시 비밀번호가 활성화되어 있으면 임시 비밀번호와 원래 비밀번호 모두 확인
        if (hasActiveTemporaryPassword) {
            // 임시 비밀번호 확인
            if (passwordEncoder.matches(currentPassword, user.getTemporaryPassword())) {
                passwordMatches = true;
            }
            // 원래 비밀번호 확인
            else if (user.getOriginalPassword() != null 
                    && passwordEncoder.matches(currentPassword, user.getOriginalPassword())) {
                passwordMatches = true;
            }
            // password 필드 확인 (임시 비밀번호로 설정되어 있을 수 있음)
            else if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                passwordMatches = true;
            }
        }
        // 임시 비밀번호가 없으면 일반 비밀번호만 확인
        else {
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                passwordMatches = true;
            }
        }
        
        return passwordMatches;
    }

    /**
     * 비밀번호 변경
     * 임시 비밀번호로 로그인한 경우에도 사용 가능 (임시 비밀번호를 현재 비밀번호로 확인)
     * 원래 비밀번호로도 변경 가능
     */
    public void changePassword(String email, PasswordChangeRequest request) {
        // 현재 비밀번호 검증
        if (!verifyPassword(email, request.getCurrentPassword())) {
            throw new GeneralException(ErrorStatus._CURRENT_PASSWORD_INCORRECT);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 새 비밀번호와 확인 일치 여부
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new GeneralException(ErrorStatus._PASSWORD_CONFIRMATION_MISMATCH);
        }

        // 비밀번호 변경: 새 비밀번호로 설정
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(newPasswordHash);
        
        // 임시 비밀번호 관련 정보 제거
        user.setOriginalPassword(null);  // originalPassword 제거
        user.setTemporaryPassword(null);  // 임시 비밀번호 제거
        user.setTemporaryPasswordExpiresAt(null);  // 만료 시간 제거
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 회원 탈퇴 (Soft Delete 방식)
     * deletedAt을 설정하여 소프트 삭제합니다.
     * 30일 후 스케줄러에 의해 하드 삭제됩니다.
     * 프로필 이미지는 즉시 S3에서 삭제됩니다.
     */
    @Transactional
    public void deleteAccount(String email, AccountDeleteRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 프로필 이미지가 있다면 S3에서 삭제
        if (user.getProfile_img() != null && !user.getProfile_img().isEmpty()) {
            try {
                s3Service.delete(List.of(user.getProfile_img()));
            } catch (Exception e) {
                log.warn("프로필 이미지 삭제 실패: {}", e.getMessage());
            }
        }

        // 탈퇴 사유 설정
        user.setWithdrawalReason(request.getReason());
        if (request.getReason() == com.fmi.domain.Enum.WithdrawalReason.OTHER) {
            user.setWithdrawalOtherReason(request.getOtherReason());
        }

        // Soft Delete (deletedAt 설정)
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("사용자 탈퇴 완료: userId={}, email={}, reason={}", user.getId(), email, request.getReason());
    }

    /**
     * 이미지 업로드 (여러 장)
     */
    public ImageUploadResponse uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new GeneralException(ErrorStatus._NOT_EXIST_FILE);
        }

        List<String> imageUrls = s3Service.upload(files);
        return UserConverter.toImageUploadResponse(imageUrls);
    }
}

