package com.fmi.domain.user.service;

import com.fmi.domain.user.repository.UserStatisticsRepository;
import com.fmi.domain.user.repository.UserStatisticsResult;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class UserStatisticsService {
    private final UserStatisticsRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public UserStatisticsResult get(LocalDate startDate, LocalDate endDate) {
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
