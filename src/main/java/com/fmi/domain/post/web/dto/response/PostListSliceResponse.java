package com.fmi.domain.post.web.dto.response;

import com.fmi.domain.post.response.PostListResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListSliceResponse {
    private List<PostListResponse> posts;
    private Long nextCursor;
    private boolean hasNext;
}

