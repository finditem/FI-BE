package com.fmi.domain.map.repository;

import com.fmi.domain.map.data.enums.MapLevel;
import com.fmi.domain.map.web.dto.response.*;
import com.fmi.domain.post.data.enums.Category;
import com.fmi.domain.post.data.enums.PostStatus;
import com.fmi.domain.post.data.enums.PostType;
import java.util.List;
import java.util.Set;

public interface PostMapCustom {

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
