package com.sisenco.weeklyreport.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private LocalDate weekStart;
    private long totalActiveMembers;
    private long totalReportsSubmitted; // SUBMITTED + APPROVED
    private double complianceRate;       // Percentage of members who submitted
    private long needsCorrectionCount;
    private long openBlockersCount;
    private long draftCount;
    private long notStartedCount;
}
