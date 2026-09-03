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
public class AchievementDto {

    private Long id;

    @NotBlank(message = "Achievement description is required")
    private String description;

    @Builder.Default
    private Boolean isKeyAchievement = false;
}
