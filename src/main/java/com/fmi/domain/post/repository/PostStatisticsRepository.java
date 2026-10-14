package com.fmi.domain.post.repository;

import com.fmi.domain.post.data.PostStatisticsDaily;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PostStatisticsRepository
        extends JpaRepository<PostStatisticsDaily, LocalDate>, PostStatisticsRepositoryCustom {
    Optional<PostStatisticsDaily> findByStartedAtIsNotNull();

    @Modifying(flushAutomatically = true)
    @Query(value = """
 INSERT INTO post_statistics (statistics_date, lost_post_count, found_post_count, removed_count)
 VALUES (:date, :lostPostCount, :foundPostCount, :removedCount)
 ON DUPLICATE KEY UPDATE lost_post_count = lost_post_count + :lostPostCount, found_post_count = found_post_count + :foundPostCount, removed_count = removed_count + :removedCount
 """, nativeQuery = true)
    void increment(
            @Param("date") LocalDate date,
            @Param("lostPostCount") long lostPostCount,
            @Param("foundPostCount") long foundPostCount,
            @Param("removedCount") long removedCount);
}
