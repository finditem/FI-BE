package com.fmi.domain.post.service;

import com.fmi.domain.post.repository.PostStatisticsRepository;
import com.fmi.domain.post.repository.PostStatisticsResult;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
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
                .orElseThrow(() -> new GeneralException(ErrorStatus._ADMIN_STATISTICS_NOT_INITIALIZED));
        if (startDate.isAfter(endDate)) throw new GeneralException(ErrorStatus._ADMIN_STATISTICS_INVALID_DATE_RANGE);
        if (startDate.isBefore(first.getStatisticsDate()))
            throw new GeneralException(ErrorStatus._ADMIN_STATISTICS_BEFORE_AVAILABLE_DATE);
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        if (endDate.isAfter(today)) throw new GeneralException(ErrorStatus._ADMIN_STATISTICS_FUTURE_END_DATE);
        return repository.aggregate(startDate, endDate);
    }
}
