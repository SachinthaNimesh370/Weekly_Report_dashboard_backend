package com.sisenco.weeklyreport.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockerDto {

    private Long id;

    @NotBlank(message = "Blocker description is required")
    private String description;

    @Builder.Default
    private Boolean isKeyIssue = false;
}
