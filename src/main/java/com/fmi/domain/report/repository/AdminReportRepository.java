package com.fmi.domain.report.repository;

import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminReportRepository extends org.springframework.data.repository.Repository<Report, Long> {

    // 관리자 전용 신고 목록 조회 - keyword 없을 때
    @Query("""
            SELECT r FROM Report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.targetType = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
            """)
    Page<Report> findAllForAdmin(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            @Param("answered") Boolean answered,
            Pageable pageable);

    // 관리자 전용 신고 목록 조회 - keyword 있을 때 (LIKE)
    @Query(value = """
            SELECT * FROM report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.target_type = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
              AND r.reason LIKE CONCAT('%', :keyword, '%')
            ORDER BY r.created_at DESC
            """, countQuery = """
            SELECT COUNT(*) FROM report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.target_type = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
              AND r.reason LIKE CONCAT('%', :keyword, '%')
            """, nativeQuery = true)
    Page<Report> findAllForAdminWithKeyword(
            @Param("status") String status,
            @Param("targetType") String targetType,
            @Param("answered") Boolean answered,
            @Param("keyword") String keyword,
            Pageable pageable);

    // 통합 목록용 커서 기반 - 전체 신고
    @Query("SELECT r FROM Report r ORDER BY r.createdAt DESC")
    Slice<Report> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r FROM Report r WHERE r.createdAt < :cursor ORDER BY r.createdAt DESC")
    Slice<Report> findAllBeforeCursorOrderByCreatedAtDesc(
            @Param("cursor") java.time.LocalDateTime cursor, Pageable pageable);

    // 관리자 커서 기반 조회 - keyword 없을 때 (JPQL)
    @Query("""
            SELECT r FROM Report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.targetType = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
              AND (:cursor IS NULL OR r.reportId < :cursor)
            ORDER BY r.reportId DESC
            """)
    List<Report> findAllForAdminCursor(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            @Param("answered") Boolean answered,
            @Param("cursor") Long cursor,
            Pageable pageable);

    // 관리자 커서 기반 조회 - keyword 있을 때 (LIKE)
    @Query(value = """
            SELECT * FROM report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.target_type = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
              AND (:cursor IS NULL OR r.report_id < :cursor)
              AND r.reason LIKE CONCAT('%', :keyword, '%')
            ORDER BY r.report_id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Report> findAllForAdminWithKeywordCursor(
            @Param("status") String status,
            @Param("targetType") String targetType,
            @Param("answered") Boolean answered,
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);
}
