package com.sisenco.weeklyreport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sisenco.weeklyreport.entity.enums.TaskPriority;
import com.sisenco.weeklyreport.entity.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "task_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnore
    private WeeklyReport report;

    @Column(name = "task_name", nullable = false, length = 255)
    private String taskName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "planned_pct")
    private Integer plannedPct;

    @Column(name = "actual_pct")
    private Integer actualPct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "time_planned_hrs", precision = 5, scale = 2)
    private BigDecimal timePlannedHrs;

    @Column(name = "time_spent_hrs", precision = 5, scale = 2)
    private BigDecimal timeSpentHrs;

    @Column(name = "output_deliverable", length = 255)
    private String outputDeliverable;
}
