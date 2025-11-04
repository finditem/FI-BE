package com.fmi.domain.favoritepost.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.favoritepost.data.FavoritePost;
import com.fmi.domain.favoritepost.repository.FavoriteRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoritePostService {

    private final PostRepository postRepository;
    private final FavoriteRepository favoriteRepository;

    @Transactional
    public void toggleFavorite(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        FavoritePost favorite = favoriteRepository.findByUserAndPost(user, post)
                .orElse(null);

        if (favorite == null) {
            favorite = PostFavorite.create(user, post);
            favoriteRepository.save(favorite);
        } else {
            favorite.toggle();
        }
    }
}
