package com.fmi.domain.post.post.repository;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import java.util.List;
import java.util.Map;

public interface PostHotQueryRepositoryCustom {
    List<Long> findHotPostIds(PostType postType, PostStatus postStatus, Category category, String address, int limit);

    Map<Long, Long> findAuthorIdsByPostIds(List<Long> postIds);
}
