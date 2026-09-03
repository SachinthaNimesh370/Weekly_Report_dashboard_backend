package com.sisenco.weeklyreport.repository;

import com.sisenco.weeklyreport.entity.Achievement;
import com.sisenco.weeklyreport.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByReport(WeeklyReport report);
}
