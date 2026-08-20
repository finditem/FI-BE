package com.fmi.domain.report.repository;

import com.fmi.domain.report.data.ReportAnswerImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportAnswerImageRepository extends JpaRepository<ReportAnswerImage, Long> {

    List<ReportAnswerImage> findByReportReportId(Long reportId);
}
