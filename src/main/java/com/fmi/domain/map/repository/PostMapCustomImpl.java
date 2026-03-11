package com.fmi.domain.map.repository;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.map.web.dto.response.MapPostResponse;
import com.fmi.domain.map.web.dto.response.PostMarkerResponse;
import com.fmi.domain.map.web.dto.response.RecentFoundPostResponse;
import com.fmi.domain.post.data.*;
import com.fmi.domain.postfavorite.data.QPostFavorite;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PostMapCustomImpl implements PostMapCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<PostMarkerResponse> findPostMaker(double lat,
                                                  double lng,
                                                  int radiusMeter,
                                                  Set<Long> excludedUserIds) {
        QPost p = QPost.post;
        double latDelta = radiusMeter / 111_320.0;
        double lngDelta = radiusMeter / (111_320.0 * Math.cos(Math.toRadians(lat)));

        double minLat = lat - latDelta;
        double maxLat = lat + latDelta;
        double minLng = lng - lngDelta;
        double maxLng = lng + lngDelta;

        NumberExpression<Double> distanceMeter = Expressions.numberTemplate(
                Double.class,
                "ST_Distance_Sphere(POINT({0},{1}), POINT({2},{3}))",
                p.longitude, p.latitude,
                lng, lat
        );

        BooleanBuilder where = new BooleanBuilder()
                .and(p.deleted.isFalse())
                .and(p.latitude.isNotNull())
                .and(p.longitude.isNotNull())
                .and(p.latitude.between(minLat, maxLat))
                .and(p.longitude.between(minLng, maxLng))
                .and(distanceMeter.loe((double) radiusMeter))
                .and(p.temporarySave.isFalse());

        if (excludedUserIds != null && !excludedUserIds.isEmpty()) {
            where.and(p.user.id.notIn(excludedUserIds));
        }

        return jpaQueryFactory
                .select(Projections.constructor(
                        PostMarkerResponse.class,
                        p.id,
                        p.latitude,
                        p.longitude,
                        p.category,
                        p.postType,
                        p.postStatus
                ))
                .from(p)
                .where(where)
                .fetch();
    }

    @Override
    public List<MapPostResponse> findMapPosts(double lat,
                                              double lng,
                                              int radiusMeter,
                                              PostType postType,
                                              PostStatus postStatus,
                                              Category category,
                                              String keyword,
                                              Long userId,
                                              Set<Long> excludedUserIds,
                                              Set<Long> hotPostIds) {

        QPost post = QPost.post;
        QPostFavorite postFavorite = QPostFavorite.postFavorite;
        QPostImage postImage = QPostImage.postImage;

        double latDelta = radiusMeter / 111_320.0;
        double lngDelta = radiusMeter / (111_320.0 * Math.cos(Math.toRadians(lat)));

        double minLat = lat - latDelta;
        double maxLat = lat + latDelta;
        double minLng = lng - lngDelta;
        double maxLng = lng + lngDelta;

        NumberExpression<Double> distanceMeter = Expressions.numberTemplate(
                Double.class,
                "ST_Distance_Sphere(POINT({0},{1}), POINT({2},{3}))",
                post.longitude, post.latitude,
                lng, lat
        );


        BooleanBuilder where = new BooleanBuilder();
        where.and(post.deleted.isFalse());
        where.and(post.temporarySave.isFalse());
        where.and(post.latitude.isNotNull());
        where.and(post.longitude.isNotNull());
        where.and(post.latitude.between(minLat, maxLat));
        where.and(post.longitude.between(minLng, maxLng));
        where.and(distanceMeter.loe((double) radiusMeter));


        if (postType != null) {
            where.and(post.postType.eq(postType));
        }
        if (postStatus != null) {
            where.and(post.postStatus.eq(postStatus));
        }
        if (category != null) {
            where.and(post.category.eq(category));
        }
        if (keyword != null && !keyword.isBlank()) {
            where.and(
                    post.title.containsIgnoreCase(keyword)
                            .or(post.content.containsIgnoreCase(keyword))
            );
        }
        if (excludedUserIds != null && !excludedUserIds.isEmpty()) {
            where.and(post.user.id.notIn(excludedUserIds));
        }

        List<Post> posts = jpaQueryFactory
                .selectFrom(post)
                .where(where)
                .orderBy(distanceMeter.asc(), post.id.desc())
                .fetch();

        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIdList = posts.stream().map(Post::getId).toList();

        Map<Long, String> thumbnailMap = jpaQueryFactory
                .select(postImage.post.id, postImage.imgUrl)
                .from(postImage)
                .where(
                        postImage.post.id.in(postIdList),
                        postImage.imageType.eq(ImageType.THUMBNAIL)
                )
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> Objects.requireNonNull(t.get(postImage.post.id)),
                        t -> Objects.requireNonNull(t.get(postImage.imgUrl)),
                        (a, b) -> a
                ));

        Map<Long, Long> favoriteCountMap = jpaQueryFactory
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

        Set<Long> myFavoritePostIds = userId == null ? Set.of() :
                new HashSet<>(
                        jpaQueryFactory
                                .select(postFavorite.post.id)
                                .from(postFavorite)
                                .where(
                                        postFavorite.user.id.eq(userId),
                                        postFavorite.post.id.in(postIdList),
                                        postFavorite.isFavorite.isTrue()
                                )
                                .fetch()
                );

        Map<Long, Integer> imageCountMap = jpaQueryFactory
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


        return posts.stream()
                .map(p -> {
                    Long pid = p.getId();
                    boolean isHot = hotPostIds != null && hotPostIds.contains(pid);

                    return new MapPostResponse(
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
                            isHot,
                            p.getCreatedAt(),
                            imageCountMap.getOrDefault(pid, 0)
                    );
                })
                .toList();
    }

    @Override
    public List<RecentFoundPostResponse> findRecentFoundPostList(double lat,
                                                                 double lng,
                                                                 int radiusMeter,
                                                                 Set<Long> excludedUserIds) {
        QPost p = QPost.post;
        QPostImage pi = QPostImage.postImage;

        double latDelta = radiusMeter / 111_320.0;
        double lngDelta = radiusMeter / (111_320.0 * Math.cos(Math.toRadians(lat)));

        double minLat = lat - latDelta;
        double maxLat = lat + latDelta;
        double minLng = lng - lngDelta;
        double maxLng = lng + lngDelta;

        NumberExpression<Double> distanceMeter = Expressions.numberTemplate(
                Double.class,
                "ST_Distance_Sphere(POINT({0},{1}), POINT({2},{3}))",
                p.longitude, p.latitude,
                lng, lat
        );

        BooleanBuilder where = new BooleanBuilder()
                .and(p.deleted.isFalse())
                .and(p.temporarySave.isFalse())
                .and(p.postType.eq(PostType.FOUND))
                .and(p.postStatus.eq(PostStatus.SEARCHING))
                .and(p.latitude.isNotNull())
                .and(p.longitude.isNotNull())
                .and(p.latitude.between(minLat, maxLat))
                .and(p.longitude.between(minLng, maxLng))
                .and(distanceMeter.loe((double) radiusMeter));

        if (excludedUserIds != null && !excludedUserIds.isEmpty()) {
            where.and(p.user.id.notIn(excludedUserIds));
        }

        return jpaQueryFactory
                .select(Projections.constructor(
                        RecentFoundPostResponse.class,
                        p.id,
                        p.title,
                        pi.imgUrl,
                        p.createdAt
                ))
                .from(p)
                .leftJoin(pi).on(
                        pi.post.eq(p),
                        pi.imageType.eq(ImageType.THUMBNAIL)
                )
                .where(where)
                .orderBy(p.createdAt.desc(), p.id.desc())
                .limit(10)
                .fetch();
    }
}
