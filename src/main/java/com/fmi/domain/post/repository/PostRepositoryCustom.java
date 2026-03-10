package com.fmi.domain.post.repository;


import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.web.dto.response.PostPageResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface PostRepositoryCustom {
    PostPageResponse searchPostsByFiltersAndSort(PostType postType,
                                                 PostStatus postStatus,
                                                 Category category,
                                                 String address,
                                                 SortType sortType,
                                                 Long cursor,
                                                 int size,
                                                 Long userId,
                                                 Set<Long> hotPostsIds);

    PostPageResponse searchMyPosts(Long userId,
                                    PostType postType,
                                    PostStatus postStatus,
                                    Category category,
                                    SortType sortType,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String keyword,
                                    Long cursor,
                                    int size);

    PostPageResponse searchMyFavorites(Long userId,
                                        PostType postType,
                                        Category category,
                                        String address,
                                        String keyword,
                                        SortType sortType,
                                        Long cursor,
                                        int size);

    List<Post> searchByKeywordWithCursor(String keyword, Long cursor, int size);

    long countByKeyword(String keyword);

    List<Post> findSimilarPosts(Long postId, int limit);
}
