package com.fmi.domain.post.post.repository;

import com.fmi.domain.post.post.service.result.PostStatisticsResult;
import java.time.LocalDate;

public interface PostStatisticsRepositoryCustom {
    PostStatisticsResult aggregate(LocalDate startDate, LocalDate endDate);
}
