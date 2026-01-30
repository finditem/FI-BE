package com.fmi.domain.user.response;

import com.fmi.domain.post.web.dto.response.PostBriefResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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
}

