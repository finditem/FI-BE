package com.fmi.domain.post.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostShareResponse {

    private String title;
    private String summary;
    private String thumbnailUrl;
}