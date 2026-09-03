package com.sisenco.weeklyreport.entity;

import com.sisenco.weeklyreport.entity.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "weekly_reports",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_week_start", columnNames = {"user_id", "week_start"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(name = "tasks_planned_next_week", columnDefinition = "TEXT")
    private String tasksPlannedNextWeek;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "current_version_no", nullable = false)
    @Builder.Default
    private Integer currentVersionNo = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskEntry> taskEntries = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Blocker> blockers = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Achievement> achievements = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HoursBreakdown> hoursBreakdowns = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReportVersion> reportVersions = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewAction> reviewActions = new ArrayList<>();

    public void addTaskEntry(TaskEntry task) {
        taskEntries.add(task);
        task.setReport(this);
    }

    public void addBlocker(Blocker blocker) {
        blockers.add(blocker);
        blocker.setReport(this);
    }

    public void addAchievement(Achievement achievement) {
        achievements.add(achievement);
        achievement.setReport(this);
    }

    public void addHoursBreakdown(HoursBreakdown hoursBreakdown) {
        hoursBreakdowns.add(hoursBreakdown);
        hoursBreakdown.setReport(this);
    }

    public void addReportVersion(ReportVersion version) {
        reportVersions.add(version);
        version.setReport(this);
    }

    public void addReviewAction(ReviewAction action) {
        reviewActions.add(action);
        action.setReport(this);
    }
}
