package com.fmi.domain.post.repository;


import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.web.dto.PostFilterDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PostRepositoryCustom {

    Slice<Post> findPostsByFilters(PostFilterDto filter, Pageable pageable, Long cursorId);
}
