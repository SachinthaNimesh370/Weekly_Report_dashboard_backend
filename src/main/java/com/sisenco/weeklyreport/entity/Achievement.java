package com.sisenco.weeklyreport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnore
    private WeeklyReport report;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "is_key_achievement", nullable = false)
    @Builder.Default
    private Boolean isKeyAchievement = false;
}
