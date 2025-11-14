package com.fmi.domain.post.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ViewResponse {
    private Long postId;
    private Long viewCount;
}