package com.fmi.domain.user.response;

import com.fmi.domain.post.web.dto.response.PostBriefResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOtherPageResponse {

    private Long userId;
    private String nickname;
    private String profileImg;
    private List<PostBriefResponse> posts;
    private List<UserCommentSummaryResponse> comments;
    private List<PostBriefResponse> favorites;
    private Long nextCursor;
    private boolean hasNext;
}
