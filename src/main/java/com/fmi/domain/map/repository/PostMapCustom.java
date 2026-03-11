package com.fmi.domain.map.repository;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.map.web.dto.response.MapPostResponse;
import com.fmi.domain.map.web.dto.response.PostMarkerResponse;
import com.fmi.domain.map.web.dto.response.RecentFoundPostResponse;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;

import java.util.List;
import java.util.Set;

public interface PostMapCustom {

    List<PostMarkerResponse> findPostMaker(
            double lat,
            double lng,
            int radiusMeter,
            Set<Long> excludedUserIds
    );

    List<MapPostResponse> findMapPosts(double lat,
                                       double lng,
                                       int radiusMeter,
                                       PostType postType,
                                       PostStatus postStatus,
                                       Category category,
                                       String keyword,
                                       Long userId,
                                       Set<Long> excludedUserIds,
                                       Set<Long> hotPostIds
    );

    List<RecentFoundPostResponse> findRecentFoundPostList(double lat,
                                                          double lng,
                                                          int radiusMeter,
                                                          Set<Long> excludedUserIds
    );
}
