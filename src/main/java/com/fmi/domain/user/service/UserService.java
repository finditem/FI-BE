package com.fmi.domain.user.service;

import com.fmi.domain.Enum.UserOtherPageType;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.user.converter.UserConverter;
import com.fmi.domain.user.response.ImageUploadResponse;
import com.fmi.domain.user.response.UserCommentSummaryResponse;
import com.fmi.domain.user.response.UserOtherPageResponse;
import com.fmi.domain.user.response.UserProfileResponse;
import com.fmi.domain.user.web.dto.AccountDeleteRequest;
import com.fmi.domain.user.web.dto.PasswordChangeRequest;
import com.fmi.domain.user.web.dto.PasswordVerifyRequest;
import com.fmi.domain.user.web.dto.ProfileImageUpdateRequest;
import com.fmi.domain.user.web.dto.UserUpdateRequest;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import com.fmi.service.EmailService;
import com.fmi.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.Collections;
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
    private final CommentRepository commentRepository;
    private final EmailService emailService;
    private final PostFavoriteRepository postFavoriteRepository;
    private final PostQueryService postQueryService;
    private final UserQueryService userQueryService;

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
     * 타인 페이지 조회 (커서 기반 페이지네이션)
     * type 파라미터에 따라 조회할 데이터를 분리합니다.
     * - posts: 게시글만 조회
     * - comments: 댓글만 조회
     * - favorites: 즐겨찾기만 조회
     * - 미지정 또는 기본값: posts (게시글만 조회)
     */
    @Transactional(readOnly = true)
    public UserOtherPageResponse getOtherUserPage(Long userId, UserOtherPageType type, UserDetails userDetails, Long cursor, int size) {

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        User me = userQueryService.findUserIfNullReturnNull(userDetails);

        // type에 따라 필요한 데이터만 조회
        List<PostBriefResponse> posts = Collections.emptyList();
        List<UserCommentSummaryResponse> comments = Collections.emptyList();
        List<PostBriefResponse> favorites = Collections.emptyList();
        Long nextCursor = null;
        boolean hasNext = false;

        UserOtherPageType resolvedType = type == null ? UserOtherPageType.POSTS : type;
        PageRequest pageRequest = PageRequest.of(0, size);

        switch (resolvedType) {
            case POSTS -> {
                Slice<Post> postSlice = (cursor == null)
                        ? postRepository.findByUserAndTemporarySaveFalseOrderByIdDesc(targetUser, pageRequest)
                        : postRepository.findByUserAndTemporarySaveFalseAndIdLessThanOrderByIdDesc(targetUser, cursor, pageRequest);

                List<Post> postList = postSlice.getContent();
                posts = postQueryService.getPostBriefResponseList(postList, me);

                hasNext = postSlice.hasNext();
                if (hasNext && !postList.isEmpty()) {
                    nextCursor = postList.get(postList.size() - 1).getId();
                }
            }
            case COMMENTS -> {
                Slice<Comment> commentSlice = (cursor == null)
                        ? commentRepository.findByUserOrderByIdDesc(targetUser, pageRequest)
                        : commentRepository.findByUserAndIdLessThanOrderByIdDesc(targetUser, cursor, pageRequest);

                List<Comment> commentList = commentSlice.getContent();
                comments = commentList.stream()
                        .map(UserConverter::toUserCommentSummaryResponse)
                        .toList();

                hasNext = commentSlice.hasNext();
                if (hasNext && !commentList.isEmpty()) {
                    nextCursor = commentList.get(commentList.size() - 1).getId();
                }
            }

            case FAVORITES -> {
                Slice<PostFavorite> favoriteSlice = (cursor == null)
                        ? postFavoriteRepository.findByUserAndIsFavoriteTrueOrderByIdDesc(targetUser, pageRequest)
                        : postFavoriteRepository.findByUserAndIsFavoriteTrueAndIdLessThanOrderByIdDesc(targetUser, cursor, pageRequest);

                List<Post> favoritePosts = favoriteSlice.getContent().stream()
                        .map(PostFavorite::getPost)
                        .toList();

                favorites = postQueryService.getPostBriefResponseList(favoritePosts, me);

                hasNext = favoriteSlice.hasNext();
                if (hasNext && !favoriteSlice.getContent().isEmpty()) {
                    nextCursor = favoriteSlice.getContent().get(favoriteSlice.getContent().size() - 1).getFavorite_id();
                }
            }
        }

        return UserConverter.toUserOtherPageResponse(targetUser, posts, comments, favorites, nextCursor, hasNext);
    }

    /**
     * 특정 사용자의 즐겨찾기 게시글 조회
     */
    private List<PostBriefResponse> getFavoritePostsByUser(User targetUser, User me) {
        List<PostFavorite> favorites = postFavoriteRepository.findByUserAndIsFavoriteTrue(targetUser);
        List<Post> postList = favorites.stream()
                .map(PostFavorite::getPost)
                .toList();

        return postQueryService.getPostBriefResponseList(postList, me);
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
     * 현재 비밀번호 검증 (프론트엔드에서 별도로 검증할 때 사용)
     */
    public void verifyPasswordWithException(String email, PasswordVerifyRequest request) {
        if (!verifyPassword(email, request.getCurrentPassword())) {
            throw new GeneralException(ErrorStatus._CURRENT_PASSWORD_INCORRECT);
        }
    }

    /**
     * 비밀번호 변경
     * 현재 비밀번호는 별도 엔드포인트에서 검증해야 하며, 이 메서드는 새 비밀번호만 받아서 변경합니다.
     */
    public void changePassword(String email, PasswordChangeRequest request) {
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

        // 계정 삭제 이메일 발송
        try {
            String nickname = user.getNickname() != null ? user.getNickname() : "회원";
            String userEmail = user.getEmail();
            String deletionDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                    .format(LocalDateTime.now());

            emailService.sendHtmlEmail(
                    userEmail,
                    "계정이 삭제되었습니다",
                    "account-deletion-email.html",
                    java.util.Map.of(
                            "NAME", nickname,
                            "USER", userEmail,
                            "DATE", deletionDate
                    )
            );
        } catch (Exception e) {
            // 이메일 발송 실패해도 계정 삭제는 성공 처리
            log.warn("계정 삭제 이메일 발송 실패: {}", e.getMessage());
        }

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

