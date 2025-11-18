package com.fmi.domain.postfavorite.converter;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.post.data.Post;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PostFavoriteConverter {

    public PostFavorite toFavoriteEntity(User user, Post post) {
        return PostFavorite.builder()
                .user(user)
                .post(post)
                .isFavorite(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
