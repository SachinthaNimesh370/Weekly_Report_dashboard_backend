package com.sisenco.weeklyreport.dto.response;

import com.sisenco.weeklyreport.entity.enums.ReportStatus;
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
public class ReportSummaryDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long projectId;
    private String projectName;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private ReportStatus status;
    private Integer currentVersionNo;
    private int taskCount;
    private int blockerCount;
    private int achievementCount;
    private String latestReviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
