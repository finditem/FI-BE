package com.fmi.domain.postfavorite.response;

public record PostFavoriteResponse(
        Long postId,
        boolean isFavorite) {
}