package com.fmi.domain.report.repository;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    // 사용자별 신고 목록
    Page<Report> findByReporter(User reporter, Pageable pageable);
    
    // 사용자별 + 상태별 신고 목록
    Page<Report> findByReporterAndStatus(User reporter, ReportStatus status, Pageable pageable);
    
    // 특정 대상에 대한 신고가 이미 있는지 확인 (중복 신고 방지)
    Optional<Report> findByReporterAndTargetTypeAndTargetId(
            User reporter, ReportTargetType targetType, Long targetId);
    
    // 신고 대상별 조회
    Page<Report> findByTargetTypeAndTargetId(
            ReportTargetType targetType, Long targetId, Pageable pageable);
}

