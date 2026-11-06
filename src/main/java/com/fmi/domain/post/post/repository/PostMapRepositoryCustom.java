package com.fmi.domain.post.post.repository;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.MapLevel;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.web.dto.response.*;
import java.util.List;
import java.util.Set;

public interface PostMapRepositoryCustom {

    List<PostMarkerResponse> findPostMarker(double lat, double lng, MapLevel mapLevel, Set<Long> excludedUserIds);

    List<PostMarkerResponse> findNearbyPostMarkers(
            double lat,
            double lng,
            MapLevel mapLevel,
            Double maxDistanceMeters,
            PostType postType,
            PostStatus postStatus,
            Category category,
            Set<Long> excludedUserIds);

    MapPostPageResponse findMapPosts(
            double lat,
            double lng,
            MapLevel mapLevel,
            PostType postType,
            PostStatus postStatus,
            Category category,
            String keyword,
            Long userId,
            Set<Long> excludedUserIds,
            Set<Long> hotPostIds,
            Double maxDistanceMeters,
            Double lastDistance,
            Long lastPostId);

    List<RecentFoundPostResponse> findRecentFoundPostList(
            double lat, double lng, MapLevel radiusMeter, Set<Long> excludedUserIds);

    LocationMapPostPageResponse searchMapPostsByLocation(
            double lat,
            double lng,
            MapLevel mapLevel,
            PostType postType,
            PostStatus postStatus,
            Category category,
            String keyword,
            Long userId,
            Set<Long> excludedUserIds,
            Set<Long> hotPostIds,
            Double lastDistance,
            Long lastPostId);
}
