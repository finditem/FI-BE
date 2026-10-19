package com.fmi.domain.place.repository;

import static com.fmi.domain.place.data.QPlace.place;
import static com.fmi.domain.place.data.QPlaceFavorite.placeFavorite;
import static com.fmi.domain.place.data.QPlaceOperationPeriod.placeOperationPeriod;

import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.global.data.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlaceFavoriteRepositoryImpl implements PlaceFavoriteRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countFavorites(Long userId, LocalDateTime now) {
        return queryFactory
                .select(placeFavorite.count())
                .from(placeFavorite)
                .join(place)
                .on(place.id.eq(placeFavorite.placeId))
                .leftJoin(placeOperationPeriod)
                .on(
                        placeOperationPeriod.place.id.eq(place.id),
                        placeOperationPeriod.entityStatus.eq(EntityStatus.ACTIVE))
                .where(
                        placeFavorite.userId.eq(userId),
                        placeFavorite.favorite.isTrue(),
                        placeFavorite.entityStatus.eq(EntityStatus.ACTIVE),
                        place.entityStatus.eq(EntityStatus.ACTIVE),
                        place.type.ne(PlaceType.POPUP).or(placeOperationPeriod.closingAt.gt(now)))
                .fetchOne();
    }
}
