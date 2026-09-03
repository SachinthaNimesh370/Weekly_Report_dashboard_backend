package com.sisenco.weeklyreport.service;

import com.sisenco.weeklyreport.dto.dashboard.ActivityFeedDto;
import com.sisenco.weeklyreport.dto.dashboard.DashboardSummaryResponse;
import com.sisenco.weeklyreport.dto.dashboard.MemberStatusDto;
import com.sisenco.weeklyreport.dto.dashboard.ProjectWorkloadDto;
import com.sisenco.weeklyreport.dto.dashboard.TasksTrendDto;
import com.sisenco.weeklyreport.dto.dashboard.TimeDistributionDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for Manager Dashboard analytics and visualizations.
 * Loosely coupled with implementation in service.impl.DashboardServiceImpl.
 */
public interface DashboardService {

    /**
     * Get summary metrics for the team (compliance, submission count, open blockers, etc.).
     */
    DashboardSummaryResponse getSummary(LocalDate weekStart);

    /**
     * Get submission status for all active team members for a given week (includes derived NOT_STARTED).
     */
    List<MemberStatusDto> getMemberStatus(LocalDate weekStart);

    /**
     * Get tasks completed trend over recent weeks.
     */
    List<TasksTrendDto> getTasksTrend(int numberOfWeeks);

    /**
     * Get workload and task count distribution by project.
     */
    List<ProjectWorkloadDto> getProjectWorkload(LocalDate weekStart);

    /**
     * Get time spent breakdown by task type (Development, Testing, Meetings, etc.).
     */
    List<TimeDistributionDto> getTimeDistribution(LocalDate weekStart);

    /**
     * Get recent activity feed (submissions, approvals, revision requests).
     */
    List<ActivityFeedDto> getRecentActivity(int limit);
}
