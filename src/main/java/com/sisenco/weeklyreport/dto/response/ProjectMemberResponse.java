package com.sisenco.weeklyreport.dto.response;

import com.sisenco.weeklyreport.entity.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {
    private Long id;
    private String fullName;
    private String email;
    private RoleName role;
    private Boolean isActive;
    private LocalDateTime joinedAt; // createdAt of User (approximation; join table has no timestamp in current ER)
}
