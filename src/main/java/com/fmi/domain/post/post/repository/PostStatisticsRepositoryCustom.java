package com.fmi.domain.post.post.repository;

import java.time.LocalDate;

public interface PostStatisticsRepositoryCustom {
    PostStatisticsResult aggregate(LocalDate startDate, LocalDate endDate);
}
