package com.fmi.domain.user.repository;

import com.fmi.domain.user.data.UserStatisticsDaily;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserStatisticsRepository
        extends JpaRepository<UserStatisticsDaily, LocalDate>, UserStatisticsRepositoryCustom {
    Optional<UserStatisticsDaily> findByStartedAtIsNotNull();

    @Modifying(flushAutomatically = true)
    @Query(value = """
 INSERT INTO user_statistics (statistics_date, opening_user_count, signup_count, withdrawal_count)
 VALUES (:date, 0, :signupCount, :withdrawalCount)
 ON DUPLICATE KEY UPDATE signup_count = signup_count + :signupCount, withdrawal_count = withdrawal_count + :withdrawalCount
 """, nativeQuery = true)
    void increment(
            @Param("date") LocalDate date,
            @Param("signupCount") long signupCount,
            @Param("withdrawalCount") long withdrawalCount);
}
