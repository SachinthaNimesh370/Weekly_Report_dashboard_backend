package com.sisenco.weeklyreport.repository;

import com.sisenco.weeklyreport.entity.Project;
import com.sisenco.weeklyreport.entity.User;
import com.sisenco.weeklyreport.entity.WeeklyReport;
import com.sisenco.weeklyreport.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long>, JpaSpecificationExecutor<WeeklyReport> {

    Optional<WeeklyReport> findByUserAndWeekStart(User user, LocalDate weekStart);

    Optional<WeeklyReport> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);

    Page<WeeklyReport> findByUser(User user, Pageable pageable);

    Page<WeeklyReport> findByUserAndStatus(User user, ReportStatus status, Pageable pageable);

    List<WeeklyReport> findByWeekStart(LocalDate weekStart);

    List<WeeklyReport> findByWeekStartAndStatus(LocalDate weekStart, ReportStatus status);

    List<WeeklyReport> findByProject(Project project);

    long countByStatus(ReportStatus status);

    long countByWeekStartAndStatus(LocalDate weekStart, ReportStatus status);
}
