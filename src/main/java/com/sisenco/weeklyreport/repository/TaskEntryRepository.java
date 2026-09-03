package com.sisenco.weeklyreport.repository;

import com.sisenco.weeklyreport.entity.TaskEntry;
import com.sisenco.weeklyreport.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskEntryRepository extends JpaRepository<TaskEntry, Long> {
    List<TaskEntry> findByReport(WeeklyReport report);
}
