package com.sisenco.weeklyreport.repository;

import com.sisenco.weeklyreport.entity.ReviewAction;
import com.sisenco.weeklyreport.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewActionRepository extends JpaRepository<ReviewAction, Long> {
    List<ReviewAction> findByReportOrderByCreatedAtDesc(WeeklyReport report);
    List<ReviewAction> findTop10ByOrderByCreatedAtDesc();
}
