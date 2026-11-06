package com.fmi.domain.admin.service;

import com.fmi.domain.admin.web.dto.response.AdminDeletedUserResponse;
import com.fmi.domain.admin.web.dto.response.AdminUserDetailResponse;
import com.fmi.domain.auth.data.enums.WithdrawalReason;
import com.fmi.domain.post.comment.repository.CommentRepository;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.UserCategory;
import com.fmi.domain.user.user.repository.AdminUserRepository;
import com.fmi.domain.user.user.repository.UserCategoryRepository;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserQueryService {
    private final AdminUserRepository adminUserRepository;

    private final ReportRepository reportRepository;

    private final UserRepository userRepository;

    private final PostRepository postRepository;

    private final CommentRepository commentRepository;

    private final UserCategoryRepository userCategoryRepository;

    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user =
                userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        long postCount = postRepository.countByUserAndDeletedFalse(user);
        long commentCount = commentRepository.countByUser(user);
        long reportCount = reportRepository.countByReporter(user);
        List<Category> categories = userCategoryRepository.findAllByUser(user).stream()
                .map(UserCategory::getCategory)
                .toList();

        return AdminUserDetailResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .emailVerified(user.isEmail_verified())
                .role(user.getRole())
                .profileImg(user.getProfile_img())
                .privacyPolicyAgreed(user.isPrivacyPolicyAgreed())
                .termsOfServiceAgreed(user.isTermsOfServiceAgreed())
                .contentPolicyAgreed(user.isContentPolicyAgreed())
                .marketingConsent(user.isMarketingConsent())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .postCount(postCount)
                .commentCount(commentCount)
                .reportCount(reportCount)
                .subscribedCategories(categories)
                .build();
    }

    /**
     * 탈퇴 유저 목록 조회
     */
    public Page<AdminDeletedUserResponse> getDeletedUsers(WithdrawalReason reason, Pageable pageable) {
        String reasonStr = reason != null ? reason.name() : null;
        Page<User> deletedUsers = adminUserRepository.findDeletedUsers(reasonStr, pageable);

        return deletedUsers.map(user -> AdminDeletedUserResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .deletedAt(user.getDeletedAt())
                .withdrawalReason(user.getWithdrawalReason())
                .withdrawalOtherReason(user.getWithdrawalOtherReason())
                .build());
    }

    public CursorPageResponse<AdminDeletedUserResponse> getDeletedUsersCursorPage(
            WithdrawalReason reason, String keyword, Long cursor, int size) {
        int fetchSize = size + 1;
        String reasonStr = reason != null ? reason.name() : null;
        String keywordParam = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim() + "%" : null;
        List<User> users =
                adminUserRepository.findDeletedUsersCursorWithKeyword(reasonStr, keywordParam, cursor, fetchSize);

        boolean hasNext = users.size() > size;
        List<User> content = hasNext ? users.subList(0, size) : users;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        List<AdminDeletedUserResponse> responseList = content.stream()
                .map(user -> AdminDeletedUserResponse.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .deletedAt(user.getDeletedAt())
                        .withdrawalReason(user.getWithdrawalReason())
                        .withdrawalOtherReason(user.getWithdrawalOtherReason())
                        .build())
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }
}
