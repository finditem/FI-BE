package com.fmi.domain.map.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.map.enumeration.MapLevel;
import com.fmi.domain.map.web.dto.request.MapPostRequest;
import com.fmi.domain.map.web.dto.request.PostMarkerRequest;
import com.fmi.domain.map.web.dto.request.RecentFoundPostRequest;
import com.fmi.domain.map.web.dto.response.MapPostResponse;
import com.fmi.domain.map.web.dto.response.PostMarkerResponse;
import com.fmi.domain.map.web.dto.response.RecentFoundPostResponse;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.HotPostService;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.userblock.repository.BlockedUserRepository;
import com.fmi.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostMapService {
    private final UserQueryService userQueryService;
    private final PostRepository postRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final PostQueryService postQueryService;
    private final HotPostService hotPostService;

    @Transactional(readOnly = true)
    public List<PostMarkerResponse> getPostMaker(PostMarkerRequest request, UserDetails userDetails) {
        MapLevel mapLevel = MapLevel.from(request.level());
        int radiusMeter = mapLevel.getRadiusMeter();

        User user = userQueryService.findUserIfNullReturnNull(userDetails);

        Set<Long> excludedUserIds = getExcludedUserIds(user);

        return postRepository.findPostMaker(
                request.latitude(),
                request.longitude(),
                radiusMeter,
                excludedUserIds
        );
    }

    @Transactional(readOnly = true)
    public List<RecentFoundPostResponse> getRecentFoundPosts(RecentFoundPostRequest request,
                                                             UserDetails userDetails) {
        MapLevel mapLevel = MapLevel.from(request.level());
        int radiusMeter = mapLevel.getRadiusMeter();

        User user = userQueryService.findUserIfNullReturnNull(userDetails);

        Set<Long> excludedUserIds = getExcludedUserIds(user);

        return postRepository.findRecentFoundPostList(
                request.latitude(),
                request.longitude(),
                radiusMeter,
                excludedUserIds
        );
    }

    @Transactional(readOnly = true)
    public List<MapPostResponse> getPostMap(Long postId, MapPostRequest request, UserDetails userDetails) {
        MapLevel mapLevel = MapLevel.from(request.level());
        int radiusMeter = mapLevel.getRadiusMeter();

        User user = userQueryService.findUserIfNullReturnNull(userDetails);
        Post post = postQueryService.findById(postId);

        Set<Long> excludedUserIds = getExcludedUserIds(user);

        Set<Long> hotPostIds = hotPostService.resolveHotPostIdsForUser(
                request.postType(),
                request.postStatus(),
                request.category(),
                post.getAddress(),
                user != null ? user.getId() : null,
                20
        );

        return postRepository.findMapPosts(
                post.getLatitude(),
                post.getLongitude(),
                radiusMeter,
                request.postType(),
                request.postStatus(),
                request.category(),
                request.keyword(),
                Objects.nonNull(user) ? user.getId() : null,
                excludedUserIds,
                hotPostIds
        );
    }

    private Set<Long> getExcludedUserIds(User user) {
        if (user == null) return Set.of();
        Set<Long> excluded = new HashSet<>();
        excluded.addAll(blockedUserRepository.findBlockedUserIdsByBlockerId(user.getId()));
        excluded.addAll(blockedUserRepository.findBlockerIdsByBlockedId(user.getId()));
        return excluded;
    }

}
