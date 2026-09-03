package com.sisenco.weeklyreport.dto.response;

import com.sisenco.weeklyreport.entity.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long projectId;
    private String projectName;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private ReportStatus status;
    private String tasksPlannedNextWeek;
    private String notes;
    private Integer currentVersionNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<TaskEntryResponse> taskEntries = new ArrayList<>();

    @Builder.Default
    private List<BlockerResponse> blockers = new ArrayList<>();

    @Builder.Default
    private List<AchievementResponse> achievements = new ArrayList<>();

    @Builder.Default
    private List<HoursBreakdownResponse> hoursBreakdowns = new ArrayList<>();

    @Builder.Default
    private List<ReviewActionResponse> reviewActions = new ArrayList<>();

    private String latestReviewComment;
}
