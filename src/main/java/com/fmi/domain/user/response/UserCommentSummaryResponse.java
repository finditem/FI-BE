package com.fmi.domain.user.response;

import com.fmi.domain.comment.web.dto.response.CommentImageResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCommentSummaryResponse {

    private Long commentId;
    private Long postId;
    private String postTitle;
    private String content;
    private long likeCount;
    private boolean isLike;
    private List<CommentImageResponse> imageList;
    private LocalDateTime createdAt;
}
