package com.fmi.domain.post.repository;

import com.fmi.domain.post.data.QPostStatisticsDaily;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostStatisticsRepositoryImpl implements PostStatisticsRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public PostStatisticsResult aggregate(LocalDate startDate, LocalDate endDate) {
        var daily = QPostStatisticsDaily.postStatisticsDaily;
        return queryFactory
                .select(Projections.constructor(
                        PostStatisticsResult.class,
                        daily.openingPostCount
                                .add(daily.lostPostCount)
                                .add(daily.foundPostCount)
                                .subtract(daily.removedCount)
                                .sum()
                                .coalesce(0L),
                        new CaseBuilder()
                                .when(daily.statisticsDate.goe(startDate))
                                .then(daily.lostPostCount)
                                .otherwise(0L)
                                .sum()
                                .coalesce(0L),
                        new CaseBuilder()
                                .when(daily.statisticsDate.goe(startDate))
                                .then(daily.foundPostCount)
                                .otherwise(0L)
                                .sum()
                                .coalesce(0L)))
                .from(daily)
                .where(daily.statisticsDate.loe(endDate))
                .fetchOne();
    }
}
