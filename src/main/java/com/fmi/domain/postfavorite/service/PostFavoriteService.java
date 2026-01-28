package com.fmi.domain.postfavorite.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.converter.PostConverter;
import com.fmi.domain.post.response.PostListResponse;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostFavoriteService {
    private final PostFavoriteRepository favoriteRepository;
    private final PostConverter postConverter;
    private final PostService postService;
    private final UserQueryService userQueryService;
    private final PostQueryService postQueryService;




    @Transactional
    public void togglePostFavorite(UserDetails userDetails, Long postId) {
        User user = userQueryService.findUser(userDetails.getUsername());
        Post post = postQueryService.findById(postId);

        PostFavorite postFavorite = favoriteRepository.findByUserAndPost(user, post).orElse(null);

        if (Objects.isNull(postFavorite)) {
            addFavorite(user, post);
            return;
        }

        postFavorite.toggleFavorite();
    }

    private void addFavorite(User user, Post post) {

        PostFavorite postFavorite = PostFavorite.create(user, post);

        favoriteRepository.save(postFavorite);
    }


    //즐찾 조회
    @Transactional(readOnly = true)
    public List<PostListResponse> getFavoritePost(UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());

        List<PostFavorite> favorites = favoriteRepository.findByUserAndIsFavoriteTrue(user);
        List<Post> posts = favorites.stream()
                .map(PostFavorite::getPost)
                .toList();

        List<Long> postIds = posts.stream().map(Post::getId).toList();

        Map<Long, Long> viewCounts = postService.getViewCountsFromRedis(postIds);

        return posts.stream()
                .map(post -> postConverter.toPostListResponse(
                        post,
                        null,
                        viewCounts.getOrDefault(post.getId(), 0L),
                        true
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Long countByPostAndIsFavoriteTrue(Post post) {
        return favoriteRepository.countByPostAndIsFavoriteTrue(post);
    }


}


