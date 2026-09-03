package com.sisenco.weeklyreport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blockers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blocker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonIgnore
    private WeeklyReport report;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "is_key_issue", nullable = false)
    @Builder.Default
    private Boolean isKeyIssue = false;
}
