package com.sisenco.weeklyreport.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockerResponse {
    private Long id;
    private String description;
    private Boolean isKeyIssue;
}
