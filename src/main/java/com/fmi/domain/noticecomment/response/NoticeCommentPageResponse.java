package com.fmi.domain.noticecomment.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoticeCommentPageResponse {
    private List<NoticeCommentResponse> comments;
    private boolean hasNext;
    private Integer nextPage;
    private long totalCommentCount;
    private long remainingCount;
}
