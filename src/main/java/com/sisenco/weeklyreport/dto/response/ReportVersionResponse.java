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
public class ReportVersionResponse {
    private Long id;
    private Integer versionNo;
    private String contentJson;
    private LocalDateTime submittedAt;
}
