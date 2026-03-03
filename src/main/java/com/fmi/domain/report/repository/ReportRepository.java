package com.fmi.domain.report.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 사용자별 신고 목록
    Page<Report> findByReporter(User reporter, Pageable pageable);

    // 사용자별 + 상태별 신고 목록
    Page<Report> findByReporterAndStatus(User reporter, ReportStatus status, Pageable pageable);

    // 사용자별 + 답변여부별 신고 목록
    Page<Report> findByReporterAndAnswered(User reporter, Boolean answered, Pageable pageable);

    // 사용자별 + 상태별 + 답변여부별 신고 목록
    Page<Report> findByReporterAndStatusAndAnswered(User reporter, ReportStatus status, Boolean answered, Pageable pageable);

    // 특정 대상에 대한 신고가 이미 있는지 확인 (중복 신고 방지)
    Optional<Report> findByReporterAndTargetTypeAndTargetId(
            User reporter, ReportTargetType targetType, Long targetId);

    // 신고 대상별 조회
    Page<Report> findByTargetTypeAndTargetId(
            ReportTargetType targetType, Long targetId, Pageable pageable);

    // 관리자 전용 신고 목록 조회 - keyword 없을 때
    @Query("""
            SELECT r FROM Report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.targetType = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
            """)
    Page<Report> findAllForAdmin(@Param("status") ReportStatus status,
                                 @Param("targetType") ReportTargetType targetType,
                                 @Param("answered") Boolean answered,
                                 Pageable pageable);

    // 관리자 전용 신고 목록 조회 - keyword 있을 때 (FULLTEXT + ngram)
    @Query(value = """
            SELECT * FROM report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.target_type = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
              AND MATCH(r.reason) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY r.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.target_type = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
              AND MATCH(r.reason) AGAINST(:keyword IN BOOLEAN MODE)
            """,
            nativeQuery = true)
    Page<Report> findAllForAdminWithKeyword(@Param("status") String status,
                                            @Param("targetType") String targetType,
                                            @Param("answered") Boolean answered,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);

    // 사용자별 커서 기반 조회 - 전체
    @Query("""
            SELECT r FROM Report r
            WHERE r.reporter = :reporter
              AND (:cursor IS NULL OR r.reportId < :cursor)
            ORDER BY r.reportId DESC
            """)
    List<Report> findByReporterCursor(@Param("reporter") User reporter,
                                      @Param("cursor") Long cursor,
                                      Pageable pageable);

    // 사용자별 커서 기반 조회 - 상태 필터
    @Query("""
            SELECT r FROM Report r
            WHERE r.reporter = :reporter AND r.status = :status
              AND (:cursor IS NULL OR r.reportId < :cursor)
            ORDER BY r.reportId DESC
            """)
    List<Report> findByReporterAndStatusCursor(@Param("reporter") User reporter,
                                                @Param("status") ReportStatus status,
                                                @Param("cursor") Long cursor,
                                                Pageable pageable);

    // 사용자별 커서 기반 조회 - 답변여부 필터
    @Query("""
            SELECT r FROM Report r
            WHERE r.reporter = :reporter AND r.answered = :answered
              AND (:cursor IS NULL OR r.reportId < :cursor)
            ORDER BY r.reportId DESC
            """)
    List<Report> findByReporterAndAnsweredCursor(@Param("reporter") User reporter,
                                                  @Param("answered") Boolean answered,
                                                  @Param("cursor") Long cursor,
                                                  Pageable pageable);

    // 사용자별 커서 기반 조회 - 상태 + 답변여부 필터
    @Query("""
            SELECT r FROM Report r
            WHERE r.reporter = :reporter AND r.status = :status AND r.answered = :answered
              AND (:cursor IS NULL OR r.reportId < :cursor)
            ORDER BY r.reportId DESC
            """)
    List<Report> findByReporterAndStatusAndAnsweredCursor(@Param("reporter") User reporter,
                                                          @Param("status") ReportStatus status,
                                                          @Param("answered") Boolean answered,
                                                          @Param("cursor") Long cursor,
                                                          Pageable pageable);

    // 사용자별 신고 목록 (createdAt 커서 기반)
    @Query("SELECT r FROM Report r WHERE r.reporter = :reporter ORDER BY r.createdAt DESC")
    Slice<Report> findByReporterOrderByCreatedAtDesc(@Param("reporter") User reporter, Pageable pageable);

    @Query("SELECT r FROM Report r WHERE r.reporter = :reporter AND r.createdAt < :cursor ORDER BY r.createdAt DESC")
    Slice<Report> findByReporterAndCreatedAtBeforeOrderByCreatedAtDesc(@Param("reporter") User reporter, @Param("cursor") java.time.LocalDateTime cursor, Pageable pageable);

    long countByReporter(User reporter);

    // 통합 목록용 커서 기반 - 전체 신고
    @Query("SELECT r FROM Report r ORDER BY r.createdAt DESC")
    Slice<Report> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r FROM Report r WHERE r.createdAt < :cursor ORDER BY r.createdAt DESC")
    Slice<Report> findAllBeforeCursorOrderByCreatedAtDesc(@Param("cursor") java.time.LocalDateTime cursor, Pageable pageable);

    // 관리자 커서 기반 조회 - keyword 없을 때 (JPQL)
    @Query("""
            SELECT r FROM Report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.targetType = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
              AND (:cursor IS NULL OR r.reportId < :cursor)
            ORDER BY r.reportId DESC
            """)
    List<Report> findAllForAdminCursor(@Param("status") ReportStatus status,
                                       @Param("targetType") ReportTargetType targetType,
                                       @Param("answered") Boolean answered,
                                       @Param("cursor") Long cursor,
                                       Pageable pageable);

    // 관리자 커서 기반 조회 - keyword 있을 때 (FULLTEXT + ngram)
    @Query(value = """
            SELECT * FROM report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.target_type = :targetType)
              AND (:answered IS NULL OR r.answered = :answered)
              AND (:cursor IS NULL OR r.report_id < :cursor)
              AND MATCH(r.reason) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY r.report_id DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<Report> findAllForAdminWithKeywordCursor(@Param("status") String status,
                                                   @Param("targetType") String targetType,
                                                   @Param("answered") Boolean answered,
                                                   @Param("keyword") String keyword,
                                                   @Param("cursor") Long cursor,
                                                   @Param("limit") int limit);
}
