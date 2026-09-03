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
public class TasksTrendDto {
    private LocalDate weekStart;
    private String label; // e.g. "Week of Sep 07"
    private long completedTasks;
    private long totalTasks;
    private double completionRate;
}
