package com.sisenco.weeklyreport.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberStatusDto {
    private Long userId;
    private String fullName;
    private String email;
    private String status; // DRAFT, SUBMITTED, NEEDS_CORRECTION, APPROVED, NOT_STARTED
    private Long reportId;
    private Integer currentVersionNo;
    private LocalDateTime lastUpdated;
}
