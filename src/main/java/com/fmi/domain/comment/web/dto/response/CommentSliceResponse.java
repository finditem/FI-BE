package com.fmi.domain.comment.web.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentSliceResponse {
    private List<CommentResponse> comments;
    private boolean hasNext;
    private Long cursor;
}
