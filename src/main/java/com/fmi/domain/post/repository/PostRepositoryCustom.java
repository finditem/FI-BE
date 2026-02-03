package com.fmi.domain.post.repository;


import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.web.dto.response.PostPageResponse;

import java.time.LocalDate;

public interface PostRepositoryCustom {
    PostPageResponse searchPostsByFiltersAndSort(PostType postType,
                                                 PostStatus postStatus,
                                                 Category category,
                                                 String address,
                                                 SortType sortType,
                                                 Long cursor,
                                                 int size,
                                                 Long userId);
}
