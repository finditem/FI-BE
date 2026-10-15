package com.fmi.domain.temperature.repository;

import com.fmi.domain.temperature.data.UserTemperature;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTemperatureRepository extends JpaRepository<UserTemperature, Long> {

    // 원장이 있는 사용자 전체를 다시 계산한다. 활동이 모두 산정 기간 밖이면 초기값으로 돌아간다.
    // 집계 결과를 upsert하는 쿼리는 QueryDSL로 표현할 수 없어 native SQL을 사용한다.
    @Modifying(clearAutomatically = true)
    @Query(value = """
                    INSERT INTO user_temperature (user_id, temperature_tenths, calculated_at)
                    SELECT calculated.user_id, calculated.temperature_tenths, :calculatedAt
                    FROM (
                        SELECT user_id,
                               LEAST(:maxTenths, GREATEST(:minTenths, :initialTenths + COALESCE(
                                   SUM(CASE WHEN occurred_at >= :from THEN delta_tenths END), 0))) AS temperature_tenths
                        FROM temperature_activity
                        GROUP BY user_id
                    ) calculated
                    ON DUPLICATE KEY UPDATE
                        temperature_tenths = calculated.temperature_tenths,
                        calculated_at = :calculatedAt
                    """, nativeQuery = true)
    int recalculateAll(
            @Param("from") LocalDateTime from,
            @Param("calculatedAt") LocalDateTime calculatedAt,
            @Param("initialTenths") int initialTenths,
            @Param("minTenths") int minTenths,
            @Param("maxTenths") int maxTenths);
}
