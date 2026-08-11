package com.fmi.domain.post.repository;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.comment.data.QComment;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.postfavorite.data.QPostFavorite;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.fmi.domain.post.data.QPost.post;

@Repository
@RequiredArgsConstructor
public class PostHotQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<Long> findHotPostIds(PostType postType,
                                     PostStatus postStatus,
                                     Category category,
                                     String address,
                                     int limit) {
        QPostFavorite postFavorite = QPostFavorite.postFavorite;
        QComment comment = QComment.comment;

        BooleanExpression where = Expressions.allOf(
                post.temporarySave.isFalse(),
                addressStartsWith(address),
                equalsPostType(postType),
                equalsPostStatus(postStatus),
                equalsCategory(category)
        );

        NumberExpression<Long> commentCount = comment.id.count();
        NumberExpression<Long> favoriteCount = postFavorite.favorite_id.count();

        return queryFactory
                .select(post.id)
                .from(post)
                .leftJoin(comment).on(
                        comment.post.eq(post),
                        comment.deleted.isFalse()
                )
                .leftJoin(postFavorite).on(
                        postFavorite.post.eq(post),
                        postFavorite.isFavorite.isTrue()
                )
                .where(where)
                .groupBy(post.id)
                .orderBy(
                        commentCount.desc(),
                        post.viewCount.desc(),
                        favoriteCount.desc(),
                        post.createdAt.desc(),
                        post.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    public Map<Long, Long> findAuthorIdsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Map.of();

        return queryFactory
                .select(post.id, post.user.id)
                .from(post)
                .where(post.id.in(postIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> Objects.requireNonNull(t.get(post.id)),
                        t -> Objects.requireNonNull(t.get(post.user.id))
                ));
    }

    private BooleanExpression addressStartsWith(String address) {
        if (address == null || address.isBlank()) return null;
        return post.address.startsWith(address);
    }

    private BooleanExpression equalsPostType(PostType postType) {
        return postType == null ? null : post.postType.eq(postType);
    }

    private BooleanExpression equalsPostStatus(PostStatus postStatus) {
        return postStatus == null ? null : post.postStatus.eq(postStatus);
    }

    private BooleanExpression equalsCategory(Category category) {
        return category == null ? null : post.category.eq(category);
    }
}
