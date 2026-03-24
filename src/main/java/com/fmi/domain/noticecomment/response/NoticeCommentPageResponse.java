package com.fmi.domain.noticecomment.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NoticeCommentPageResponse {
    private List<NoticeCommentResponse> comments;
    private boolean hasNext;
    private Integer nextPage;
    private long totalCommentCount;
    private long remainingCount;
}
