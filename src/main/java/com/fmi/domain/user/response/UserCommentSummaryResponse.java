package com.fmi.domain.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCommentSummaryResponse {

    private Long commentId;
    private Long postId;
    private String postTitle;
    private String content;
    private LocalDateTime createdAt;
}

