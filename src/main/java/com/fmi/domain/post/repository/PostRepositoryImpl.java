package com.fmi.domain.post.repository;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.post.data.*;
import com.fmi.domain.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.post.web.dto.response.PostPageResponse;
import com.fmi.domain.postfavorite.data.QPostFavorite;
import com.fmi.domain.userblock.repository.BlockedUserRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static com.fmi.domain.post.data.QPostImage.postImage;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final BlockedUserRepository blockedUserRepository;

    @Override
    public PostPageResponse searchPostsByFiltersAndSort(PostType postType, PostStatus postStatus, Category category, String address, SortType sortType, Long cursor, int size, Long userId) {
        QPost post = QPost.post;
        QPostFavorite postFavorite = QPostFavorite.postFavorite;

        BooleanExpression whereQuery = Expressions.allOf(
                addressStartsWith(post, address),
                equalsPostType(post, postType),
                equalsPostStatus(post, postStatus),
                equalsCategory(post, category),
                cursorCondition(post, sortType, cursor),
                excludeBlockedUsers(post, userId)
        );

        List<Post> posts;
        if (Objects.equals(sortType, SortType.MOST_FAVORITED)) {
            posts = queryFactory.select(post)
                    .from(post).leftJoin(postFavorite).on(
                            postFavorite.post.eq(post),
                            postFavorite.isFavorite.isTrue()
                    ).where(whereQuery)
                    .groupBy(post.id)
                    .orderBy(
                            postFavorite.favorite_id.count().desc(),
                            post.id.desc()
                    )
                    .limit(size + 1)
                    .fetch();
        } else {
            posts = queryFactory
                    .selectFrom(post)
                    .where(whereQuery)
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
            return new PostPageResponse(List.of(), null, false);
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

                    // TODO isHot 기준
                    boolean isHot = favCnt >= 10 || p.getViewCount() >= 100;

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

        return new PostPageResponse(postList, nextCursor, hasNext);
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
