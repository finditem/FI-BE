package com.fmi.domain.comment.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CommentSliceResponse {
    private List<CommentResponse> comments;
    private boolean hasNext;
    private Long cursor;
}
