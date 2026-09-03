package com.sisenco.weeklyreport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean isActive;
    private int memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
