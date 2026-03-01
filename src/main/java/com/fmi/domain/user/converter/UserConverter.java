package com.fmi.domain.user.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.user.response.ImageUploadResponse;
import com.fmi.domain.user.response.UserCommentSummaryResponse;
import com.fmi.domain.user.response.UserOtherPageResponse;
import com.fmi.domain.user.response.UserProfileResponse;
import com.fmi.domain.user.web.dto.response.UserCommentResponse;
import com.fmi.domain.user.web.dto.response.UserPostResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserConverter {

    /**
     * User Entity → UserProfileResponse 변환
     * 닉네임, 이메일, 프로필 이미지만 반환
     */
    public static UserProfileResponse toUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImg(user.getProfile_img())
                .role(user.getRole())
                .build();
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
            List<PostBriefResponse> posts,
            List<UserCommentSummaryResponse> comments,
            List<PostBriefResponse> favorites
    ) {
        return UserOtherPageResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfile_img())
                .posts(posts)
                .comments(comments)
                .favorites(favorites)
                .build();
    }

    public static UserOtherPageResponse toUserOtherPageResponse(
            User user,
            List<PostBriefResponse> posts,
            List<UserCommentSummaryResponse> comments,
            List<PostBriefResponse> favorites,
            Long nextCursor,
            boolean hasNext
    ) {
        return UserOtherPageResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImg(user.getProfile_img())
                .posts(posts)
                .comments(comments)
                .favorites(favorites)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    public static UserPostResponse toUserPostResponse(User user, long postCount, long chattingCount) {
        return new UserPostResponse(
                user.getId(),
                user.getNickname(),
                user.getProfile_img(),
                postCount,
                chattingCount
        );
    }

    public static UserCommentResponse toUserCommentResponse(User user) {
        return new UserCommentResponse(
                user.getId(),
                user.getNickname(),
                user.getProfile_img()
        );
    }
}

