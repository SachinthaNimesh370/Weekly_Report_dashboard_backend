package com.sisenco.weeklyreport.dto.request;

import com.sisenco.weeklyreport.entity.enums.TaskType;
import jakarta.validation.constraints.DecimalMin;
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
public class HoursBreakdownDto {

    private Long id;

    @NotNull(message = "Task type is required")
    private TaskType taskType;

    @NotNull(message = "Hours are required")
    @DecimalMin(value = "0.0", message = "Hours cannot be negative")
    private BigDecimal hours;
}
