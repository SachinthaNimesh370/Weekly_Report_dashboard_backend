package com.sisenco.weeklyreport.controller;

import com.sisenco.weeklyreport.dto.dashboard.*;
import com.sisenco.weeklyreport.dto.response.ApiResponse;
import com.sisenco.weeklyreport.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    // ─────────────────────────────────────────────
    // 1. SUMMARY METRICS
    // ─────────────────────────────────────────────
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week
    ) {
        DashboardSummaryResponse response = dashboardService.getSummary(week);
        return ResponseEntity.ok(ApiResponse.success(response, "Dashboard summary retrieved successfully"));
    }

    // ─────────────────────────────────────────────
    // 2. MEMBER SUBMISSION STATUS (INCL. NOT_STARTED)
    // ─────────────────────────────────────────────
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<MemberStatusDto>>> getMemberStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week
    ) {
        List<MemberStatusDto> response = dashboardService.getMemberStatus(week);
        return ResponseEntity.ok(ApiResponse.success(response, "Member status breakdown retrieved"));
    }

    // ─────────────────────────────────────────────
    // 3. TASKS COMPLETED TREND CHART
    // ─────────────────────────────────────────────
    @GetMapping("/tasks-trend")
    public ResponseEntity<ApiResponse<List<TasksTrendDto>>> getTasksTrend(
            @RequestParam(defaultValue = "6") int weeks
    ) {
        List<TasksTrendDto> response = dashboardService.getTasksTrend(weeks);
        return ResponseEntity.ok(ApiResponse.success(response, "Tasks trend data retrieved"));
    }

    // ─────────────────────────────────────────────
    // 4. PROJECT WORKLOAD DISTRIBUTION CHART
    // ─────────────────────────────────────────────
    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectWorkloadDto>>> getProjectWorkload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week
    ) {
        List<ProjectWorkloadDto> response = dashboardService.getProjectWorkload(week);
        return ResponseEntity.ok(ApiResponse.success(response, "Project workload distribution retrieved"));
    }

    // ─────────────────────────────────────────────
    // 5. TIME DISTRIBUTION BY TASK TYPE CHART
    // ─────────────────────────────────────────────
    @GetMapping("/time-distribution")
    public ResponseEntity<ApiResponse<List<TimeDistributionDto>>> getTimeDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week
    ) {
        List<TimeDistributionDto> response = dashboardService.getTimeDistribution(week);
        return ResponseEntity.ok(ApiResponse.success(response, "Time distribution retrieved"));
    }

    // ─────────────────────────────────────────────
    // 6. RECENT ACTIVITY FEED
    // ─────────────────────────────────────────────
    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<ActivityFeedDto>>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ActivityFeedDto> response = dashboardService.getRecentActivity(limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Recent activity feed retrieved"));
    }
}
