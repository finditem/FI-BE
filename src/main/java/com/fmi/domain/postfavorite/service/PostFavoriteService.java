package com.fmi.domain.postfavorite.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.post.converter.PostConverter;
import com.fmi.domain.post.response.PostListResponse;
import com.fmi.domain.postfavorite.converter.PostFavoriteConverter;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostFavoriteService {

    private final PostRepository postRepository;
    private final PostFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PostFavoriteConverter favoritePostConverter;
    private final PostConverter postConverter;

    @Transactional
    public boolean toggleFavorite(Long postId, UserDetails userDetails) {
        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        PostFavorite favorite = favoriteRepository.findByUserAndPost(user, post)
                .orElse(null);

        if (favorite == null) {
            favoriteRepository.save(favoritePostConverter.toFavoriteEntity(user,post));
            return true;
        }

        favorite.toggle();

        return favorite.isFavorite();
    }

    @Transactional(readOnly = true)
    public List<PostListResponse> getFavoritePost(UserDetails userDetails) {

        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다."));

        List<PostFavorite> favorites = favoriteRepository.findByUserAndIsFavoriteTrue(user);

        return favorites.stream()
                .map(PostFavorite::getPost)
                .map(postConverter::toPostListResponse)
                .toList();
    }


}
