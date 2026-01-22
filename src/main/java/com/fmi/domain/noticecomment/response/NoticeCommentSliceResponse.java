package com.fmi.domain.noticecomment.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NoticeCommentSliceResponse {
    private List<NoticeCommentResponse> comments;
    private boolean hasNext;
    private Long cursor;
}
