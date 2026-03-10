package com.fmi.domain.noticecomment.response;

import com.fmi.domain.user.web.dto.response.UserCommentResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCommentResponse {
    private Long id;
    private String content;
    private boolean deleted;
    private int depth;
    private LocalDateTime createdAt;
    private UserCommentResponse authorResponse;
    private long childCommentCount;
    private List<NoticeCommentImageResponse> imageList;
    private int likeCount;
    private boolean isLike;
    private boolean canEdit;
    private boolean canDelete;
}
