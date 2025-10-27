package com.fmi.domain.post.response;

import lombok.Builder;

@Builder
public class PostShareResponse {

    private String url;
    private String title;
    private String summary;
    private String image;
}