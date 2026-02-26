package com.fmi.domain.post.repository;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.comment.data.QComment;
import com.fmi.domain.post.data.*;
import com.fmi.domain.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.post.web.dto.response.PostPageResponse;
import com.fmi.domain.postfavorite.data.QPostFavorite;
import com.fmi.domain.userblock.repository.BlockedUserRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.fmi.domain.post.data.QPostImage.postImage;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final BlockedUserRepository blockedUserRepository;

    @Override
    public PostPageResponse searchPostsByFiltersAndSort(PostType postType,
                                                        PostStatus postStatus,
                                                        Category category,
                                                        String address,
                                                        SortType sortType,
                                                        Long cursor,
                                                        int size,
                                                        Long userId,
                                                        Set<Long> hotPostsIds) {
        QPost post = QPost.post;
        QPostFavorite postFavorite = QPostFavorite.postFavorite;

        BooleanExpression baseWhere = Expressions.allOf(
                post.deleted.isFalse(),
                addressStartsWith(post, address),
                equalsPostType(post, postType),
                equalsPostStatus(post, postStatus),
                equalsCategory(post, category),
                excludeBlockedUsers(post, userId)
        );

        BooleanExpression pageWhere = Expressions.allOf(
                baseWhere,
                cursorCondition(post, sortType, cursor)
        );

        Long postCount = queryFactory
                .select(post.id.countDistinct())
                .from(post)
                .leftJoin(postFavorite).on(
                        postFavorite.post.eq(post),
                        postFavorite.isFavorite.isTrue()
                )
                .where(baseWhere)
                .fetchOne();

        if (Objects.isNull(postCount)) postCount = 0L;

        List<Post> posts;
        if (Objects.equals(sortType, SortType.MOST_FAVORITED)) {
            posts = queryFactory.select(post)
                    .from(post)
                    .leftJoin(postFavorite).on(
                            postFavorite.post.eq(post),
                            postFavorite.isFavorite.isTrue()
                    )
                    .where(pageWhere)
                    .groupBy(post.id)
                    .orderBy(postFavorite.favorite_id.count().desc(), post.id.desc())
                    .limit(size + 1)
                    .fetch();
        } else {
            posts = queryFactory
                    .selectFrom(post)
                    .where(pageWhere)
                    .orderBy(orderBySortType(post, sortType))
                    .limit(size + 1)
                    .fetch();
        }

        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts = posts.subList(0, size);
        }

        Long nextCursor = (hasNext && !posts.isEmpty()) ? posts.get(posts.size() - 1).getId() : null;

        if (posts.isEmpty()) {
            return new PostPageResponse(List.of(), 0L, null, false);
        }

        List<Long> postIdList = posts.stream().map(Post::getId).toList();

        Map<Long, String> thumbnailMap = queryFactory
                .select(postImage.post.id, postImage.id, postImage.imgUrl)
                .from(postImage)
                .where(
                        postImage.post.id.in(postIdList),
                        postImage.imageType.eq(ImageType.THUMBNAIL)
                )
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> Objects.requireNonNull(t.get(postImage.post.id)),
                        t -> Objects.requireNonNull(t.get(postImage.imgUrl))
                ));

        Map<Long, Long> favoriteCountMap = queryFactory
                .select(postFavorite.post.id, postFavorite.favorite_id.count())
                .from(postFavorite)
                .where(
                        postFavorite.post.id.in(postIdList),
                        postFavorite.isFavorite.isTrue()
                )
                .groupBy(postFavorite.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> Objects.requireNonNull(t.get(postFavorite.post.id)),
                        t -> Objects.requireNonNull(t.get(postFavorite.favorite_id.count()))
                ));


        Set<Long> myFavoritePostIds = Objects.isNull(userId) ? Set.of() :
                new HashSet<>(
                        queryFactory
                                .select(postFavorite.post.id)
                                .from(postFavorite)
                                .where(
                                        postFavorite.user.id.eq(userId),
                                        postFavorite.post.id.in(postIdList),
                                        postFavorite.isFavorite.isTrue()
                                )
                                .fetch()
                );

        Map<Long, Integer> imageCountMap = queryFactory
                .select(postImage.post.id, postImage.id.count())
                .from(postImage)
                .where(postImage.post.id.in(postIdList))
                .groupBy(postImage.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> Objects.requireNonNull(t.get(postImage.post.id)),
                        t -> Objects.requireNonNull(t.get(postImage.id.count())).intValue()
                ));


        List<PostBriefResponse> postList = posts.stream()
                .map(p -> {
                    Long pid = p.getId();
                    long favCnt = favoriteCountMap.getOrDefault(pid, 0L);

                    boolean isNew = p.isNew();
                    boolean isHot = hotPostsIds != null && hotPostsIds.contains(pid);


                    return new PostBriefResponse(
                            pid,
                            p.getTitle(),
                            p.makeSummary(),
                            thumbnailMap.get(pid),
                            p.getAddress(),
                            p.getPostStatus(),
                            p.getPostType(),
                            p.getCategory(),
                            favCnt,
                            myFavoritePostIds.contains(pid),
                            p.getViewCount(),
                            isNew,
                            isHot,
                            p.getCreatedAt(),
                            imageCountMap.getOrDefault(pid, 0)
                    );
                })
                .toList();

        return new PostPageResponse(postList, postCount, nextCursor, hasNext);
    }

    @Override
    public List<Post> searchByKeywordWithCursor(String keyword, Long cursor, int size) {
        QPost post = QPost.post;

        BooleanBuilder where = new BooleanBuilder();

        if (hasText(keyword)) {
            where.and(buildKeywordCondition(post, keyword));
        } else {
            return List.of();
        }

        if (Objects.nonNull(cursor)) {
            where.and(post.id.lt(cursor));
        }

        where.and(post.deleted.isFalse());
        where.and(post.temporarySave.isFalse());

        return queryFactory
                .selectFrom(post)
                .where(where)
                .orderBy(post.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public long countByKeyword(String keyword) {
        QPost post = QPost.post;

        if (!hasText(keyword)) return 0L;

        BooleanBuilder where = new BooleanBuilder()
                .and(buildKeywordCondition(post, keyword));

        where.and(post.deleted.isFalse());
        where.and(post.temporarySave.isFalse());

        Long count = queryFactory
                .select(post.id.count())
                .from(post)
                .where(where)
                .fetchOne();

        return count == null ? 0L : count;
    }

    private BooleanBuilder buildKeywordCondition(QPost post, String keyword) {
        String[] tokens = keyword.split("\\s+");

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        for (String token : tokens) {
            if (!hasText(token)) continue;

            booleanBuilder.or(
                    post.title.containsIgnoreCase(token)
                            .or(post.content.containsIgnoreCase(token))
            );
        }
        return booleanBuilder;
    }


    @Override
    public List<Post> findSimilarPosts(Long postId, int limit) {
        QPost post = QPost.post;

        Post base = queryFactory.selectFrom(post)
                .where(post.id.eq(postId))
                .fetchOne();

        if (Objects.isNull(base)) {
            return List.of();
        }

        LocalDateTime baseDate = Objects.nonNull(base.getDate()) ? base.getDate() : base.getCreatedAt();
        LocalDateTime from = baseDate.minusDays(7);
        LocalDateTime to = baseDate.plusDays(7);

        PostType oppositeType = Objects.equals(base.getPostType(), PostType.LOST)
                ? PostType.FOUND
                : PostType.LOST;

        BooleanExpression dateCondition =
                post.date.isNotNull().and(post.date.between(from, to))
                        .or(post.date.isNull().and(post.createdAt.between(from, to)));

        return queryFactory
                .selectFrom(post)
                .where(
                        post.deleted.isFalse(),
                        post.temporarySave.isFalse(),
                        post.id.ne(postId),
                        post.temporarySave.isFalse(),
                        post.category.eq(base.getCategory()),
                        post.postType.eq(oppositeType),
                        post.postStatus.eq(PostStatus.SEARCHING),
                        post.address.eq(base.getAddress()),
                        dateCondition
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(limit)
                .fetch();
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

    private BooleanExpression equalsPostStatus(QPost post, PostStatus postStatus) {
        return Objects.isNull(postStatus) ? null : post.postStatus.eq(postStatus);
    }

    private BooleanExpression equalsCategory(QPost post, Category category) {
        return Objects.isNull(category) ? null : post.category.eq(category);
    }

    private OrderSpecifier<?>[] orderBySortType(QPost post, SortType sortType) {
        return switch (sortType) {
            case LATEST -> new OrderSpecifier[]{
                    post.createdAt.desc(),
                    post.id.desc()
            };

            case OLDEST -> new OrderSpecifier[]{
                    post.createdAt.asc(),
                    post.id.asc()
            };

            case MOST_VIEWED -> new OrderSpecifier[]{
                    post.viewCount.desc(),
                    post.id.desc()
            };

            case MOST_FAVORITED -> new OrderSpecifier[]{
                    post.id.desc()
            };
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

    private BooleanExpression excludeBlockedUsers(QPost post, Long userId) {
        if (Objects.isNull(userId)) {
            return null;
        }

        // 1. 내가 차단한 사람들
        List<Long> blockedUserIds = blockedUserRepository.findBlockedUserIdsByBlockerId(userId);

        // 2. 나를 차단한 사람들
        List<Long> blockerIds = blockedUserRepository.findBlockerIdsByBlockedId(userId);

        // 3. 두 목록 합치기 (양방향 차단)
        Set<Long> excludedUserIds = new HashSet<>();
        excludedUserIds.addAll(blockedUserIds);
        excludedUserIds.addAll(blockerIds);

        if (excludedUserIds.isEmpty()) {
            return null;
        }

        return post.user.id.notIn(excludedUserIds);
    }

}
