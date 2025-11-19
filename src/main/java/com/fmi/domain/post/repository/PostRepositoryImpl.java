package com.fmi.domain.post.repository;

import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.QPost;
import com.fmi.domain.post.web.dto.PostFilterDto;
import com.querydsl.jpa.impl.JPAQuery;
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

//        QPost p = QPost.post;
//
//        JPAQuery<Post> query = queryFactory
//                .selectFrom(p)
//                .where(
//                        filter.getCategory() != null ? p.category.eq(filter.getCategory()) : null,
//                        filter.getAddress() != null ? p.address.eq(filter.getAddress()) : null,
//                        filter.getItemStatus() != null ? p.itemStatus.eq(filter.getItemStatus()) : null,
//                        filter.getStartDate() != null ? p.createdAt.goe(filter.getStartDate().atStartOfDay()) : null,
//                        filter.getEndDate() != null ? p.createdAt.loe(filter.getEndDate().atTime(23,59,59)) : null
//                )
//                .orderBy(
//                        switch(filter.getSortType()) {
//                            case OLDEST -> p.createdAt.asc();
//                            case LATEST -> p.createdAt.desc();
//                            case MOST_FAVORITED -> p.favoriteCount.desc();
//                            default -> p.viewCnt.desc();
//                        }
//                )
//                .limit(pageable.getPageSize() + 1);


        return null;
    }


}
