package com.fmi.domain.user.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.post.response.PostListResponse;
import com.fmi.domain.user.response.ImageUploadResponse;
import com.fmi.domain.user.response.UserCommentSummaryResponse;
import com.fmi.domain.user.response.UserOtherPageResponse;
import com.fmi.domain.user.response.UserProfileResponse;
import com.fmi.domain.user.web.dto.UserUpdateRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserConverter {

    /**
     * User Entity → UserProfileResponse 변환
     * 닉네임, 이메일(인증여부), 프로필 이미지만 반환
     */
    public static UserProfileResponse toUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .emailVerified(user.isEmail_verified())
                .profileImg(user.getProfile_img())
                .build();
    }

    /**
     * UserUpdateRequest로부터 User Entity 업데이트
     * 닉네임만 수정 가능
     */
    public static void updateUserFromRequest(User user, UserUpdateRequest request) {
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
    }

    /**
     * 이미지 업로드 결과 → ImageUploadResponse 변환
     */
    public static ImageUploadResponse toImageUploadResponse(List<String> imageUrls) {
        return ImageUploadResponse.builder()
                .imageUrls(imageUrls)
                .build();
    }

    public static UserCommentSummaryResponse toUserCommentSummaryResponse(Comment comment) {
        return UserCommentSummaryResponse.builder()
                .commentId(comment.getId())
                .postId(comment.getPost().getId())
                .postTitle(comment.getPost().getTitle())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    public static UserOtherPageResponse toUserOtherPageResponse(
            User user,
            List<PostListResponse> posts,
            List<UserCommentSummaryResponse> comments
    ) {
        return UserOtherPageResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfile_img())
                .posts(posts)
                .comments(comments)
                .build();
    }
}

