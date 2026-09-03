package com.sisenco.weeklyreport.dto.request;

import com.sisenco.weeklyreport.entity.enums.TaskPriority;
import com.sisenco.weeklyreport.entity.enums.TaskStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntryDto {

    private Long id;

    @NotBlank(message = "Task name is required")
    private String taskName;

    @NotNull(message = "Task priority is required")
    private TaskPriority priority;

    @Min(value = 0, message = "Planned % must be at least 0")
    @Max(value = 100, message = "Planned % cannot exceed 100")
    private Integer plannedPct;

    @Min(value = 0, message = "Actual % must be at least 0")
    @Max(value = 100, message = "Actual % cannot exceed 100")
    private Integer actualPct;

    @NotNull(message = "Task status is required")
    private TaskStatus status;

    @DecimalMin(value = "0.0", message = "Planned hours cannot be negative")
    private BigDecimal timePlannedHrs;

    @DecimalMin(value = "0.0", message = "Spent hours cannot be negative")
    private BigDecimal timeSpentHrs;

    private String outputDeliverable;
}
