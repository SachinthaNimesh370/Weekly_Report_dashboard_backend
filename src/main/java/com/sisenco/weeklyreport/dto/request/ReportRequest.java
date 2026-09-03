package com.sisenco.weeklyreport.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Week start date is required")
    private LocalDate weekStart;

    @NotNull(message = "Week end date is required")
    private LocalDate weekEnd;

    private String tasksPlannedNextWeek;

    private String notes;

    @Valid
    @Builder.Default
    private List<TaskEntryDto> taskEntries = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<BlockerDto> blockers = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<AchievementDto> achievements = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<HoursBreakdownDto> hoursBreakdowns = new ArrayList<>();
}
