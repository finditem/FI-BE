package com.fmi.domain.post.post.repository;

import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.global.enums.SortType;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface PostSearchRepositoryCustom {
    PostPageResponse searchPostsByFiltersAndSort(
            PostType postType,
            PostStatus postStatus,
            Category category,
            String address,
            SortType sortType,
            Long cursor,
            int size,
            Long userId,
            Set<Long> hotPostsIds);

    PostPageResponse searchMyPosts(
            Long userId,
            PostType postType,
            PostStatus postStatus,
            Category category,
            SortType sortType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Long cursor,
            int size);

    List<Post> searchByKeywordWithCursor(String keyword, Long cursor, int size);

    long countByKeyword(String keyword);

    List<Post> findSimilarPosts(Long postId, int limit);
}
