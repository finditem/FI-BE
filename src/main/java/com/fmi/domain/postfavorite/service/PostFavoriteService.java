package com.fmi.domain.postfavorite.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.post.converter.PostConverter;
import com.fmi.domain.post.response.PostListResponse;
import com.fmi.domain.post.service.PostService;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostFavoriteService {

    private final PostRepository postRepository;
    private final PostFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PostFavoriteConverter favoritePostConverter;
    private final PostConverter postConverter;
    private final PostService postService;


    @Transactional
    public boolean addFavorite(Long postId, UserDetails userDetails){

        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        boolean exists = favoriteRepository.existsByUserAndPost(user, post);
        if (exists) {
            return true;
        }

        favoriteRepository.save(
                favoritePostConverter.toFavoriteEntity(user, post)
        );

        post.setFavoriteCount(post.getFavoriteCount() + 1);

        return true;
    }

    @Transactional
    public boolean deleteFavorite(Long postId, UserDetails userDetails){

        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._POST_NOT_FOUND));

        PostFavorite favorite = favoriteRepository.findByUserAndPost(user, post)
                .orElse(null);

        if (favorite == null) {
            return false;
        }

        favoriteRepository.delete(favorite);


        post.setFavoriteCount(post.getFavoriteCount() - 1);

        return false;
    }

    //즐찾 조회
    @Transactional(readOnly = true)
    public List<PostListResponse> getFavoritePost(UserDetails userDetails) {

        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

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



}


