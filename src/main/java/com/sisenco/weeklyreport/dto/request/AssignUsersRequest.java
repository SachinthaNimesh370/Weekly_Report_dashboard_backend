package com.sisenco.weeklyreport.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignUsersRequest {

    @NotEmpty(message = "At least one user ID must be provided")
    private Set<Long> userIds;
}
