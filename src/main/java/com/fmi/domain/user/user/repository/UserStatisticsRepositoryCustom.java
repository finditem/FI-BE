package com.fmi.domain.user.user.repository;

import com.fmi.domain.user.user.service.result.UserStatisticsResult;
import java.time.LocalDate;

public interface UserStatisticsRepositoryCustom {
    UserStatisticsResult aggregate(LocalDate startDate, LocalDate endDate);
}
