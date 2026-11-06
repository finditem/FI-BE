package com.fmi.domain.post.post.repository;

import static com.fmi.domain.post.post.data.QPostImage.postImage;

import com.fmi.domain.post.post.data.*;
import com.fmi.domain.post.post.data.QPostFavorite;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.ImageType;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.global.enums.SortType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostFavoriteRepositoryCustomImpl implements PostFavoriteRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public PostPageResponse searchMyFavorites(
            Long userId,
            PostType postType,
            Category category,
            String address,
            String keyword,
            SortType sortType,
            Long cursor,
            int size) {
        QPost post = QPost.post;
        QPostFavorite postFavorite = QPostFavorite.postFavorite;

        BooleanBuilder baseBuilder = new BooleanBuilder();
        baseBuilder.and(postFavorite.user.id.eq(userId));
        baseBuilder.and(postFavorite.isFavorite.isTrue());
        baseBuilder.and(post.deleted.isFalse());
        baseBuilder.and(equalsPostType(post, postType));
        baseBuilder.and(equalsCategory(post, category));
        baseBuilder.and(addressStartsWith(post, address));

        if (hasText(keyword)) {
            baseBuilder.and(buildKeywordCondition(post, keyword));
        }

        BooleanBuilder pageWhere = new BooleanBuilder(baseBuilder);
        pageWhere.and(cursorCondition(post, sortType, cursor));

        Long postCount = queryFactory
                .select(post.id.countDistinct())
                .from(postFavorite)
                .join(postFavorite.post, post)
                .where(baseBuilder)
                .fetchOne();

        if (Objects.isNull(postCount)) postCount = 0L;

        List<Post> posts;
        if (Objects.equals(sortType, SortType.MOST_FAVORITED)) {
            QPostFavorite favCount = new QPostFavorite("favCount");
            posts = queryFactory
                    .select(post)
                    .from(postFavorite)
                    .join(postFavorite.post, post)
                    .leftJoin(favCount)
                    .on(favCount.post.eq(post), favCount.isFavorite.isTrue())
                    .where(pageWhere)
                    .groupBy(post.id)
                    .orderBy(favCount.favorite_id.count().desc(), post.id.desc())
                    .limit(size + 1)
                    .fetch();
        } else {
            posts = queryFactory
                    .select(post)
                    .from(postFavorite)
                    .join(postFavorite.post, post)
                    .where(pageWhere)
                    .orderBy(orderBySortType(post, sortType))
                    .limit(size + 1)
                    .fetch();
        }

        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts = posts.subList(0, size);
        }

        Long nextCursor =
                (hasNext && !posts.isEmpty()) ? posts.get(posts.size() - 1).getId() : null;

        if (posts.isEmpty()) {
            return new PostPageResponse(List.of(), 0L, null, false);
        }

        return buildPostPageResponse(posts, postCount, nextCursor, hasNext, userId);
    }

    private PostPageResponse buildPostPageResponse(
            List<Post> posts, Long postCount, Long nextCursor, boolean hasNext, Long userId) {
        QPostFavorite postFavorite = QPostFavorite.postFavorite;
        List<Long> postIdList = posts.stream().map(Post::getId).toList();

        // 목록 썸네일 우선, 없으면 원본 URL로 대체
        Expression<String> listThumbnailUrl = postImage.thumbnailUrl.coalesce(postImage.imgUrl);
        Map<Long, String> thumbnailMap = queryFactory
                .select(postImage.post.id, listThumbnailUrl)
                .from(postImage)
                .where(postImage.post.id.in(postIdList), postImage.imageType.eq(ImageType.THUMBNAIL))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> Objects.requireNonNull(t.get(postImage.post.id)),
                        t -> Objects.requireNonNull(t.get(listThumbnailUrl)),
                        (a, b) -> a));

        Map<Long, Long> favoriteCountMap = queryFactory
                .select(postFavorite.post.id, postFavorite.favorite_id.count())
                .from(postFavorite)
                .where(postFavorite.post.id.in(postIdList), postFavorite.isFavorite.isTrue())
                .groupBy(postFavorite.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> Objects.requireNonNull(t.get(postFavorite.post.id)),
                        t -> Objects.requireNonNull(t.get(postFavorite.favorite_id.count()))));

        Set<Long> myFavoritePostIds = Objects.isNull(userId)
                ? Set.of()
                : new HashSet<>(queryFactory
                        .select(postFavorite.post.id)
                        .from(postFavorite)
                        .where(
                                postFavorite.user.id.eq(userId),
                                postFavorite.post.id.in(postIdList),
                                postFavorite.isFavorite.isTrue())
                        .fetch());

        Map<Long, Integer> imageCountMap = queryFactory
                .select(postImage.post.id, postImage.id.count())
                .from(postImage)
                .where(postImage.post.id.in(postIdList))
                .groupBy(postImage.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> Objects.requireNonNull(t.get(postImage.post.id)),
                        t -> Objects.requireNonNull(t.get(postImage.id.count())).intValue()));

        List<PostBriefResponse> postList = posts.stream()
                .map(p -> {
                    Long pid = p.getId();
                    return new PostBriefResponse(
                            pid,
                            p.getTitle(),
                            p.makeSummary(),
                            thumbnailMap.get(pid),
                            p.getAddress(),
                            p.getPostStatus(),
                            p.getPostType(),
                            p.getCategory(),
                            favoriteCountMap.getOrDefault(pid, 0L),
                            myFavoritePostIds.contains(pid),
                            p.getViewCount(),
                            p.isNew(),
                            false,
                            p.getCreatedAt(),
                            imageCountMap.getOrDefault(pid, 0));
                })
                .toList();

        return new PostPageResponse(postList, postCount, nextCursor, hasNext);
    }

    private BooleanBuilder buildKeywordCondition(QPost post, String keyword) {
        String[] tokens = keyword.split("\\s+");

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        for (String token : tokens) {
            if (!hasText(token)) continue;

            booleanBuilder.or(post.title.containsIgnoreCase(token).or(post.content.containsIgnoreCase(token)));
        }
        return booleanBuilder;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BooleanExpression addressStartsWith(QPost post, String address) {
        if (Objects.isNull(address) || address.isBlank()) {
            return null;
        }

        return post.address.startsWith(address);
    }

    private BooleanExpression equalsPostType(QPost post, PostType postType) {
        return Objects.isNull(postType) ? null : post.postType.eq(postType);
    }

    private BooleanExpression equalsCategory(QPost post, Category category) {
        return Objects.isNull(category) ? null : post.category.eq(category);
    }

    private OrderSpecifier<?>[] orderBySortType(QPost post, SortType sortType) {
        return switch (sortType) {
            case LATEST -> new OrderSpecifier[] {post.createdAt.desc(), post.id.desc()};

            case OLDEST -> new OrderSpecifier[] {post.createdAt.asc(), post.id.asc()};

            case MOST_VIEWED -> new OrderSpecifier[] {post.viewCount.desc(), post.id.desc()};

            case MOST_FAVORITED -> new OrderSpecifier[] {post.id.desc()};
        };
    }

    private BooleanExpression cursorCondition(QPost post, SortType sortType, Long cursor) {
        if (Objects.isNull(cursor)) {
            return null;
        }

        return switch (sortType) {
            case LATEST, MOST_VIEWED -> post.id.lt(cursor);
            case OLDEST -> post.id.gt(cursor);
            case MOST_FAVORITED -> null;
        };
    }
}
