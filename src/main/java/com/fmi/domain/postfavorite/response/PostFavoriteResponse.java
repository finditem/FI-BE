package com.fmi.domain.postfavorite.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostFavoriteResponse {
    private Long postId;
    private boolean isFavorite;
}