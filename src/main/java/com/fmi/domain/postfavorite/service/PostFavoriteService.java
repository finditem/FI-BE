package com.fmi.domain.postfavorite.service;

import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.postfavorite.response.PostFavoriteResponse;
import com.fmi.domain.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.UserQueryService;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostFavoriteService {
    private final PostFavoriteRepository favoriteRepository;
    private final UserQueryService userQueryService;
    private final PostRepository postRepository;

    @Transactional
    public PostFavoriteResponse addFavorite(Long postId, UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());
        Post post =
                postRepository.findById(postId).orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));
        PostFavorite postFavorite =
                favoriteRepository.findByUserAndPost(user, post).orElse(null);

        if (Objects.isNull(postFavorite)) {
            try {
                favoriteRepository.save(PostFavorite.create(user, post));
            } catch (DataIntegrityViolationException exception) {
                postFavorite = favoriteRepository.findByUserAndPost(user, post).orElseThrow();
                postFavorite.activate();
            }
        } else {
            postFavorite.activate();
        }

        return new PostFavoriteResponse(post.getId(), true);
    }

    @Transactional
    public PostFavoriteResponse deleteFavorite(Long postId, UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());
        Post post =
                postRepository.findById(postId).orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        PostFavorite postFavorite = favoriteRepository
                .findByUserAndPost(user, post)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_FAVORITE_NOT_FOUND));

        postFavorite.deactivate();

        return new PostFavoriteResponse(post.getId(), false);
    }

    @Transactional(readOnly = true)
    public Long countByPostAndIsFavoriteTrue(Post post) {
        return favoriteRepository.countByPostAndIsFavoriteTrue(post);
    }

    @Transactional(readOnly = true)
    public boolean isFavoritePostByUser(User user, Post post) {
        return favoriteRepository
                .findByUserAndPost(user, post)
                .map(PostFavorite::isFavorite)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> getIsFavoriteMap(User user, List<Post> posts) {
        if (posts.isEmpty() || Objects.isNull(user)) return Collections.emptyMap();

        List<Long> postIdList = posts.stream().map(Post::getId).toList();

        Set<Long> favoritePostIdSet =
                new HashSet<>(favoriteRepository.findFavoritePostIdsByUserAndPostIds(user, postIdList));

        return postIdList.stream().collect(Collectors.toMap(id -> id, favoritePostIdSet::contains));
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getFavoriteCountMap(List<Post> posts) {
        if (posts.isEmpty()) return Collections.emptyMap();

        return favoriteRepository.countFavoritesByPosts(posts).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
