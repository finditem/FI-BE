package com.fmi.domain.postfavorite.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.postfavorite.data.PostFavorite;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostFavoriteService {
    private final PostFavoriteRepository favoriteRepository;
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
    public List<PostBriefResponse> getFavoritePost(UserDetails userDetails) {
        User user = userQueryService.findUser(userDetails.getUsername());

        List<PostFavorite> favorites = favoriteRepository.findByUserAndIsFavoriteTrue(user);
        List<Post> posts = favorites.stream()
                .map(PostFavorite::getPost)
                .toList();

        return postQueryService.getPostBriefResponseList(posts, user);
    }

    @Transactional(readOnly = true)
    public Long countByPostAndIsFavoriteTrue(Post post) {
        return favoriteRepository.countByPostAndIsFavoriteTrue(post);
    }

    @Transactional(readOnly = true)
    public boolean isFavoritePostByUser(User user, Post post) {
        return favoriteRepository.findByUserAndPost(user, post)
                .map(PostFavorite::isFavorite)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> getIsFavoriteMap(User user, List<Post> posts) {
        if (posts.isEmpty() || Objects.isNull(user)) return Collections.emptyMap();

        List<Long> postIdList = posts.stream().map(Post::getId).toList();

        Set<Long> favoritePostIdSet = new HashSet<>(
                favoriteRepository.findFavoritePostIdsByUserAndPostIds(user, postIdList)
        );

        return postIdList.stream().collect(Collectors.toMap(
                id -> id,
                favoritePostIdSet::contains
        ));
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getFavoriteCountMap(List<Post> posts) {
        if (posts.isEmpty()) return Collections.emptyMap();

        return favoriteRepository.countFavoritesByPosts(posts).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }


}


