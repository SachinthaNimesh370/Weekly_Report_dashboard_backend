package com.sisenco.weeklyreport.dto.dashboard;

import com.sisenco.weeklyreport.entity.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFeedDto {
    private Long id;
    private String actionType; // "APPROVED", "CHANGES_REQUESTED", "SUBMITTED"
    private String title;
    private String actorName;
    private RoleName actorRole;
    private Long reportId;
    private LocalDate weekStart;
    private String projectName;
    private String comment;
    private LocalDateTime timestamp;
}
