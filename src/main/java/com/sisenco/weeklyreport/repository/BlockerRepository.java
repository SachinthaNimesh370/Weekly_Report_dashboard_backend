package com.sisenco.weeklyreport.repository;

import com.sisenco.weeklyreport.entity.Blocker;
import com.sisenco.weeklyreport.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockerRepository extends JpaRepository<Blocker, Long> {
    List<Blocker> findByReport(WeeklyReport report);
    long countByIsKeyIssueTrue();
}
