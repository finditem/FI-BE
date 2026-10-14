package com.fmi.domain.user.data;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_statistics")
@Getter
@NoArgsConstructor
public class UserStatisticsDaily {
    @Id
    @Column(name = "statistics_date", nullable = false)
    private LocalDate statisticsDate;

    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "started_at", unique = true)
    private LocalDateTime startedAt;

    @Column(name = "opening_user_count", nullable = false)
    private long openingUserCount;

    @Column(name = "signup_count", nullable = false)
    private long signupCount;

    @Column(name = "withdrawal_count", nullable = false)
    private long withdrawalCount;
}
