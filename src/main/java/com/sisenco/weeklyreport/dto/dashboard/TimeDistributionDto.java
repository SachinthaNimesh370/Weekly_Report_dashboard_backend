package com.sisenco.weeklyreport.dto.dashboard;

import com.sisenco.weeklyreport.entity.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeDistributionDto {
    private TaskType taskType;
    private String label; // e.g. "Development", "Testing", "Meetings"
    private BigDecimal totalHours;
    private double percentage;
}
