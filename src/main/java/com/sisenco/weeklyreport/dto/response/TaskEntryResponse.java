package com.sisenco.weeklyreport.dto.response;

import com.sisenco.weeklyreport.entity.enums.TaskPriority;
import com.sisenco.weeklyreport.entity.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntryResponse {
    private Long id;
    private String taskName;
    private TaskPriority priority;
    private Integer plannedPct;
    private Integer actualPct;
    private TaskStatus status;
    private BigDecimal timePlannedHrs;
    private BigDecimal timeSpentHrs;
    private String outputDeliverable;
}
