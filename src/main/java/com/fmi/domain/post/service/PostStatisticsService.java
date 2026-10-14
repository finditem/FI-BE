package com.fmi.domain.post.service;

import com.fmi.domain.post.exception.PostStatisticsErrorStatus;
import com.fmi.domain.post.repository.PostStatisticsRepository;
import com.fmi.domain.post.repository.PostStatisticsResult;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class PostStatisticsService {
    private final PostStatisticsRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PostStatisticsResult get(LocalDate startDate, LocalDate endDate) {
        var first = repository
                .findByStartedAtIsNotNull()
                .orElseThrow(() -> new GeneralException(PostStatisticsErrorStatus.NOT_INITIALIZED));
        if (startDate.isAfter(endDate)) throw new GeneralException(PostStatisticsErrorStatus.INVALID_DATE_RANGE);
        if (startDate.isBefore(first.getStatisticsDate()))
            throw new GeneralException(PostStatisticsErrorStatus.BEFORE_AVAILABLE_DATE);
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        if (endDate.isAfter(today)) throw new GeneralException(PostStatisticsErrorStatus.FUTURE_END_DATE);
        return repository.aggregate(startDate, endDate);
    }
}
