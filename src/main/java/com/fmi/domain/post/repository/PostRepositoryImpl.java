package com.fmi.domain.post.repository;

import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.web.dto.PostFilterDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Post> findPostsByFilters(PostFilterDto filter, Pageable pageable){


        return null;
    }


}
