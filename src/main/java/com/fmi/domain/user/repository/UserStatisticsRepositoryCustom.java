package com.fmi.domain.user.repository;

import java.time.LocalDate;

public interface UserStatisticsRepositoryCustom {
    UserStatisticsResult aggregate(LocalDate startDate, LocalDate endDate);
}
