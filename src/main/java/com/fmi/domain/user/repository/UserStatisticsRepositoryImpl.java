package com.fmi.domain.user.repository;

import com.fmi.domain.user.data.QUserStatisticsDaily;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserStatisticsRepositoryImpl implements UserStatisticsRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public UserStatisticsResult aggregate(LocalDate startDate, LocalDate endDate) {
        var daily = QUserStatisticsDaily.userStatisticsDaily;
        return queryFactory
                .select(Projections.constructor(
                        UserStatisticsResult.class,
                        daily.openingUserCount
                                .add(daily.signupCount)
                                .subtract(daily.withdrawalCount)
                                .sum()
                                .coalesce(0L),
                        new CaseBuilder()
                                .when(daily.statisticsDate.goe(startDate))
                                .then(daily.signupCount)
                                .otherwise(0L)
                                .sum()
                                .coalesce(0L),
                        new CaseBuilder()
                                .when(daily.statisticsDate.goe(startDate))
                                .then(daily.withdrawalCount)
                                .otherwise(0L)
                                .sum()
                                .coalesce(0L)))
                .from(daily)
                .where(daily.statisticsDate.loe(endDate))
                .fetchOne();
    }
}
