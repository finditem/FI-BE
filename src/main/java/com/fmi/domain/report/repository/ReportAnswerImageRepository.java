package com.fmi.domain.report.repository;

import com.fmi.domain.report.data.ReportAnswerImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportAnswerImageRepository extends JpaRepository<ReportAnswerImage, Long> {

    List<ReportAnswerImage> findByReportReportId(Long reportId);
}
