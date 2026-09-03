package com.sisenco.weeklyreport.repository;

import com.sisenco.weeklyreport.entity.HoursBreakdown;
import com.sisenco.weeklyreport.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoursBreakdownRepository extends JpaRepository<HoursBreakdown, Long> {
    List<HoursBreakdown> findByReport(WeeklyReport report);
}
