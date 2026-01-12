package com.fmi.domain.post.repository;

import com.fmi.domain.Enum.SortType;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.QPost;
import com.fmi.domain.post.web.dto.PostFilterDto;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Post> findPostsByFilters(PostFilterDto dto, Pageable pageable, Long cursorId){

        QPost p = QPost.post;

        Post cursor = null;
        if (cursorId != null) {
            cursor = queryFactory.selectFrom(p)
                    .where(p.id.eq(cursorId))
                    .fetchOne();
        }

        JPAQuery<Post> query = queryFactory
                .selectFrom(p)
                .where(
                        dto.getCategory() != null ? p.category.eq(dto.getCategory()) : null,
                        dto.getAddress() != null ? p.address.containsIgnoreCase(dto.getAddress()) : null,
                        dto.getItemStatus() != null ? p.itemStatus.eq(dto.getItemStatus()) : null,
                        dto.getStartDate() != null ? p.createdAt.goe(dto.getStartDate().atStartOfDay()) : null,
                        dto.getEndDate() != null ? p.createdAt.loe(dto.getEndDate().atTime(23,59,59)) : null,
                        cursorCondition(cursor, dto.getSortType(), p)
                )
                .orderBy(getOrderBy(dto.getSortType(), p))
                .limit(pageable.getPageSize() + 1);


        List<Post> content = query.fetch();
        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) content.remove(pageable.getPageSize());

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private OrderSpecifier<?>[] getOrderBy(SortType sortType, QPost p) {
        return switch (sortType) {
            case OLDEST -> new OrderSpecifier[]{p.createdAt.asc(), p.id.asc()};
            case LATEST -> new OrderSpecifier[]{p.createdAt.desc(), p.id.desc()};
            case MOST_FAVORITED -> new OrderSpecifier[]{p.favoriteCount.desc(), p.id.desc()};
            case MOST_VIEWED -> new OrderSpecifier[]{p.viewCnt.desc(), p.id.desc()};
            default -> new OrderSpecifier[]{p.id.desc()};
        };
    }

    private BooleanExpression cursorCondition(Post cursor, SortType sortType, QPost p) {
        if (cursor == null) return null;

        return switch (sortType) {
            case OLDEST ->
                    p.createdAt.gt(cursor.getCreatedAt())
                            .or(p.createdAt.eq(cursor.getCreatedAt())
                                    .and(p.id.gt(cursor.getId())));
            case LATEST ->
                    p.createdAt.lt(cursor.getCreatedAt())
                            .or(p.createdAt.eq(cursor.getCreatedAt())
                                    .and(p.id.lt(cursor.getId())));
            case MOST_FAVORITED ->
                    p.favoriteCount.lt(cursor.getFavoriteCount())
                            .or(p.favoriteCount.eq(cursor.getFavoriteCount())
                                .and(p.id.lt(cursor.getId())));
            case MOST_VIEWED ->
                    p.viewCnt.lt(cursor.getViewCnt())
                            .or(p.viewCnt.eq(cursor.getViewCnt())
                                .and(p.id.lt(cursor.getId())));

        };
    }

}
