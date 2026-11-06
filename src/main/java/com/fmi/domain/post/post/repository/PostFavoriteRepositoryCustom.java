package com.fmi.domain.post.post.repository;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.global.enums.SortType;

public interface PostFavoriteRepositoryCustom {
    PostPageResponse searchMyFavorites(
            Long userId,
            PostType postType,
            Category category,
            String address,
            String keyword,
            SortType sortType,
            Long cursor,
            int size);
}
