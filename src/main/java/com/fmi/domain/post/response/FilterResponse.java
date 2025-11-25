package com.fmi.domain.post.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class FilterResponse {

    private List<PostListResponse> posts;
    private boolean hasNext;
    private Long nextCursor;

}
