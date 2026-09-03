package com.sisenco.weeklyreport.repository;

import com.sisenco.weeklyreport.entity.ReportVersion;
import com.sisenco.weeklyreport.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportVersionRepository extends JpaRepository<ReportVersion, Long> {
    List<ReportVersion> findByReportOrderByVersionNoDesc(WeeklyReport report);
    Optional<ReportVersion> findByReportAndVersionNo(WeeklyReport report, Integer versionNo);
}
