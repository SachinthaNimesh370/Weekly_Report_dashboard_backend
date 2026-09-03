package com.sisenco.weeklyreport.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectWorkloadDto {
    private Long projectId;
    private String label; // Project Name (chart-ready)
    private long taskCount;
    private long reportCount;
    private BigDecimal totalHours;
}
