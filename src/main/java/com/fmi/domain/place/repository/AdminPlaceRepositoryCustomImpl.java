package com.fmi.domain.place.repository;

import static com.fmi.domain.place.data.QPlace.place;
import static com.fmi.domain.place.data.QPlaceTranslation.placeTranslation;

import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.service.result.AdminPlaceResult;
import com.fmi.domain.user.user.data.enums.LanguageCode;
import com.fmi.global.data.EntityStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdminPlaceRepositoryCustomImpl implements AdminPlaceRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminPlaceResult> findAll(
            EntityStatus status, PlaceType type, String keyword, LocalDateTime cursorTime, Long cursorId, int limit) {
        // 정렬 커서 기준: 일반 목록 -> 등록 시각, 삭제 목록 -> 삭제 시각
        DateTimePath<LocalDateTime> sortTime = place.createdAt;
        if (status == EntityStatus.DELETED) {
            sortTime = place.deletedAt;
        }
        // 엔티티 상태: ACTIVE, DELETED
        BooleanBuilder conditions = new BooleanBuilder(place.entityStatus.eq(status));
        // 장소 유형: null -> 전체, 특정 장소 타입 -> 타입 적용
        if (type != null) {
            conditions.and(place.type.eq(type));
        }
        // 검색어 키워드 있을 때 검색 적용 -> LIKE 패턴 적용
        if (keyword != null && !keyword.isBlank()) {
            // 입력한 %, _, !를 LIKE 패턴 문자가 아닌 일반 문자로 취급한다.
            String escaped =
                    keyword.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
            String pattern = "%" + escaped + "%";
            StringExpression normalizedPattern = Expressions.asString(pattern).lower();
            conditions.and(place.name
                    .lower()
                    .like(normalizedPattern, '!')
                    .or(placeTranslation.translatedName.lower().like(normalizedPattern, '!')));
        }
        // sortTime: 조회 커서 기준, cursorTime: 현재 커서 어디인지
        // 커서 동일하다면 id 기준 내림차순
        if (cursorTime != null) {
            conditions.and(sortTime.lt(cursorTime).or(sortTime.eq(cursorTime).and(place.id.lt(cursorId))));
        }
        return queryFactory
                .select(Projections.constructor(
                        AdminPlaceResult.class,
                        place.id,
                        place.name,
                        placeTranslation.translatedName,
                        place.type,
                        place.address,
                        place.thumbnailUrl,
                        place.stationDistanceMeters,
                        place.createdAt,
                        place.deletedAt))
                .from(place)
                // 영문 정보가 없는 장소도 유지하고 영문 원본을 함께 조회한다.
                .leftJoin(placeTranslation)
                .on(placeTranslation.place.eq(place), placeTranslation.languageCode.eq(LanguageCode.EN))
                .where(conditions)
                .orderBy(sortTime.desc(), place.id.desc())
                // 다음 페이지 존재 여부를 판단할 여분 한 건까지 조회한다.
                .limit(limit)
                .fetch();
    }
}
