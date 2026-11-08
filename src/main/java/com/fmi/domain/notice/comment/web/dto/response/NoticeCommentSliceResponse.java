package com.fmi.domain.notice.comment.web.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoticeCommentSliceResponse {
    private List<NoticeCommentResponse> comments;
    private boolean hasNext;
    private Long cursor;
}
