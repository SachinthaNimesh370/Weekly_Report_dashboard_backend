package com.sisenco.weeklyreport.dto.response;

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
public class HoursBreakdownResponse {
    private Long id;
    private TaskType taskType;
    private BigDecimal hours;
}
