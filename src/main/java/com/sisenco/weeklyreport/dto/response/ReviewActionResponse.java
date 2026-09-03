package com.sisenco.weeklyreport.dto.response;

import com.sisenco.weeklyreport.entity.enums.ReviewActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewActionResponse {
    private Long id;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerEmail;
    private Integer againstVersionNo;
    private ReviewActionType action;
    private String comment;
    private LocalDateTime createdAt;
}
