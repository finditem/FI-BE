package com.fmi.domain.post.data;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "post_statistics")
@Getter
@NoArgsConstructor
public class PostStatisticsDaily {
    @Id
    @Column(name = "statistics_date", nullable = false)
    private LocalDate statisticsDate;

    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "started_at", unique = true)
    private LocalDateTime startedAt;

    @Column(name = "opening_post_count", nullable = false)
    private long openingPostCount;

    @Column(name = "lost_post_count", nullable = false)
    private long lostPostCount;

    @Column(name = "found_post_count", nullable = false)
    private long foundPostCount;

    @Column(name = "removed_count", nullable = false)
    private long removedCount;
}
