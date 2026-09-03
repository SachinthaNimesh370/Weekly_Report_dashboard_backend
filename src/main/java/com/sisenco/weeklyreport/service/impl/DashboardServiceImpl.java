package com.sisenco.weeklyreport.service.impl;

import com.sisenco.weeklyreport.dto.dashboard.*;
import com.sisenco.weeklyreport.entity.*;
import com.sisenco.weeklyreport.entity.enums.ReportStatus;
import com.sisenco.weeklyreport.entity.enums.RoleName;
import com.sisenco.weeklyreport.entity.enums.TaskStatus;
import com.sisenco.weeklyreport.entity.enums.TaskType;
import com.sisenco.weeklyreport.repository.*;
import com.sisenco.weeklyreport.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ReviewActionRepository reviewActionRepository;
    private final ReportVersionRepository reportVersionRepository;

    // ─────────────────────────────────────────────
    // 1. DASHBOARD SUMMARY
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDate weekStart) {
        LocalDate targetWeek = resolveTargetWeek(weekStart);
        log.info("Generating dashboard summary for week: {}", targetWeek);

        List<User> activeMembers = userRepository.findByIsActiveTrue().stream()
                .filter(u -> u.getRole() != null && u.getRole().getName() == RoleName.ROLE_TEAM_MEMBER)
                .collect(Collectors.toList());

        long totalActiveMembers = activeMembers.size();
        List<WeeklyReport> reports = weeklyReportRepository.findByWeekStart(targetWeek);

        long submittedCount = reports.stream()
                .filter(r -> r.getStatus() == ReportStatus.SUBMITTED || r.getStatus() == ReportStatus.APPROVED)
                .count();

        long needsCorrectionCount = reports.stream()
                .filter(r -> r.getStatus() == ReportStatus.NEEDS_CORRECTION)
                .count();

        long draftCount = reports.stream()
                .filter(r -> r.getStatus() == ReportStatus.DRAFT)
                .count();

        long notStartedCount = Math.max(0, totalActiveMembers - reports.size());

        double complianceRate = totalActiveMembers > 0
                ? BigDecimal.valueOf((submittedCount * 100.0) / totalActiveMembers)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue()
                : 0.0;

        long openBlockers = reports.stream()
                .filter(r -> r.getStatus() != ReportStatus.DRAFT)
                .flatMap(r -> r.getBlockers().stream())
                .count();

        return DashboardSummaryResponse.builder()
                .weekStart(targetWeek)
                .totalActiveMembers(totalActiveMembers)
                .totalReportsSubmitted(submittedCount)
                .complianceRate(complianceRate)
                .needsCorrectionCount(needsCorrectionCount)
                .openBlockersCount(openBlockers)
                .draftCount(draftCount)
                .notStartedCount(notStartedCount)
                .build();
    }

    // ─────────────────────────────────────────────
    // 2. MEMBER STATUS BREAKDOWN
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MemberStatusDto> getMemberStatus(LocalDate weekStart) {
        LocalDate targetWeek = resolveTargetWeek(weekStart);
        log.info("Fetching member status breakdown for week: {}", targetWeek);

        List<User> activeMembers = userRepository.findByIsActiveTrue().stream()
                .filter(u -> u.getRole() != null && u.getRole().getName() == RoleName.ROLE_TEAM_MEMBER)
                .collect(Collectors.toList());

        List<WeeklyReport> reports = weeklyReportRepository.findByWeekStart(targetWeek);
        Map<Long, WeeklyReport> reportByUser = reports.stream()
                .collect(Collectors.toMap(r -> r.getUser().getId(), r -> r, (r1, r2) -> r1));

        return activeMembers.stream().map(member -> {
            WeeklyReport report = reportByUser.get(member.getId());
            if (report != null) {
                return MemberStatusDto.builder()
                        .userId(member.getId())
                        .fullName(member.getFullName())
                        .email(member.getEmail())
                        .status(report.getStatus().name())
                        .reportId(report.getId())
                        .currentVersionNo(report.getCurrentVersionNo())
                        .lastUpdated(report.getUpdatedAt() != null ? report.getUpdatedAt() : report.getCreatedAt())
                        .build();
            } else {
                return MemberStatusDto.builder()
                        .userId(member.getId())
                        .fullName(member.getFullName())
                        .email(member.getEmail())
                        .status("NOT_STARTED")
                        .reportId(null)
                        .currentVersionNo(null)
                        .lastUpdated(null)
                        .build();
            }
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 3. TASKS TREND (OVER RECENT WEEKS)
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TasksTrendDto> getTasksTrend(int numberOfWeeks) {
        int weeks = numberOfWeeks > 0 ? numberOfWeeks : 6;
        LocalDate currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<LocalDate> weekStarts = new ArrayList<>();

        for (int i = weeks - 1; i >= 0; i--) {
            weekStarts.add(currentMonday.minusWeeks(i));
        }

        return weekStarts.stream().map(week -> {
            List<WeeklyReport> reports = weeklyReportRepository.findByWeekStart(week);

            long totalTasks = reports.stream()
                    .flatMap(r -> r.getTaskEntries().stream())
                    .count();

            long completedTasks = reports.stream()
                    .flatMap(r -> r.getTaskEntries().stream())
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();

            double rate = totalTasks > 0
                    ? BigDecimal.valueOf((completedTasks * 100.0) / totalTasks)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue()
                    : 0.0;

            String label = "W" + week.getDayOfMonth() + " " + week.getMonth().name().substring(0, 3);

            return TasksTrendDto.builder()
                    .weekStart(week)
                    .label(label)
                    .completedTasks(completedTasks)
                    .totalTasks(totalTasks)
                    .completionRate(rate)
                    .build();
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 4. PROJECT WORKLOAD DISTRIBUTION
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ProjectWorkloadDto> getProjectWorkload(LocalDate weekStart) {
        log.info("Computing project workload distribution");
        List<Project> activeProjects = projectRepository.findByIsActiveTrue();

        return activeProjects.stream().map(project -> {
            List<WeeklyReport> reports = weeklyReportRepository.findByProject(project);

            if (weekStart != null) {
                reports = reports.stream()
                        .filter(r -> r.getWeekStart().equals(weekStart))
                        .collect(Collectors.toList());
            }

            long taskCount = reports.stream()
                    .mapToLong(r -> r.getTaskEntries().size())
                    .sum();

            BigDecimal totalHours = reports.stream()
                    .flatMap(r -> r.getTaskEntries().stream())
                    .map(TaskEntry::getTimeSpentHrs)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return ProjectWorkloadDto.builder()
                    .projectId(project.getId())
                    .label(project.getName())
                    .taskCount(taskCount)
                    .reportCount(reports.size())
                    .totalHours(totalHours)
                    .build();
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 5. TIME DISTRIBUTION BY TASK TYPE
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TimeDistributionDto> getTimeDistribution(LocalDate weekStart) {
        log.info("Computing time distribution across task types");

        List<WeeklyReport> reports;
        if (weekStart != null) {
            reports = weeklyReportRepository.findByWeekStart(weekStart);
        } else {
            reports = weeklyReportRepository.findAll();
        }

        Map<TaskType, BigDecimal> hoursByType = new EnumMap<>(TaskType.class);
        for (TaskType type : TaskType.values()) {
            hoursByType.put(type, BigDecimal.ZERO);
        }

        for (WeeklyReport report : reports) {
            for (HoursBreakdown hb : report.getHoursBreakdowns()) {
                if (hb.getTaskType() != null && hb.getHours() != null) {
                    hoursByType.put(
                            hb.getTaskType(),
                            hoursByType.get(hb.getTaskType()).add(hb.getHours())
                    );
                }
            }
        }

        BigDecimal grandTotal = hoursByType.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Arrays.stream(TaskType.values()).map(type -> {
            BigDecimal hours = hoursByType.get(type);
            double percentage = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? hours.multiply(BigDecimal.valueOf(100))
                    .divide(grandTotal, 1, RoundingMode.HALF_UP)
                    .doubleValue()
                    : 0.0;

            String readableLabel = capitalize(type.name());

            return TimeDistributionDto.builder()
                    .taskType(type)
                    .label(readableLabel)
                    .totalHours(hours)
                    .percentage(percentage)
                    .build();
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 6. RECENT ACTIVITY FEED
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ActivityFeedDto> getRecentActivity(int limit) {
        int maxResults = limit > 0 ? limit : 10;
        List<ActivityFeedDto> activities = new ArrayList<>();

        // Add review actions
        List<ReviewAction> recentReviews = reviewActionRepository.findTop10ByOrderByCreatedAtDesc();
        for (ReviewAction ra : recentReviews) {
            String title = ra.getAction() == com.sisenco.weeklyreport.entity.enums.ReviewActionType.APPROVED
                    ? "Report Approved"
                    : "Correction Requested";

            activities.add(ActivityFeedDto.builder()
                    .id(ra.getId())
                    .actionType(ra.getAction().name())
                    .title(title)
                    .actorName(ra.getReviewer().getFullName())
                    .actorRole(ra.getReviewer().getRole().getName())
                    .reportId(ra.getReport().getId())
                    .weekStart(ra.getReport().getWeekStart())
                    .projectName(ra.getReport().getProject().getName())
                    .comment(ra.getComment())
                    .timestamp(ra.getCreatedAt())
                    .build());
        }

        // Add recent submissions
        List<ReportVersion> recentSubmissions = reportVersionRepository.findAll().stream()
                .sorted(Comparator.comparing(ReportVersion::getSubmittedAt).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());

        for (ReportVersion rv : recentSubmissions) {
            activities.add(ActivityFeedDto.builder()
                    .id(rv.getId())
                    .actionType("SUBMITTED")
                    .title("Report Submitted (v" + rv.getVersionNo() + ")")
                    .actorName(rv.getReport().getUser().getFullName())
                    .actorRole(rv.getReport().getUser().getRole().getName())
                    .reportId(rv.getReport().getId())
                    .weekStart(rv.getReport().getWeekStart())
                    .projectName(rv.getReport().getProject().getName())
                    .comment("Version " + rv.getVersionNo() + " submitted for manager review")
                    .timestamp(rv.getSubmittedAt())
                    .build());
        }

        return activities.stream()
                .sorted(Comparator.comparing(ActivityFeedDto::getTimestamp).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    private LocalDate resolveTargetWeek(LocalDate weekStart) {
        if (weekStart != null) {
            return weekStart;
        }
        // Default to current week's Monday
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase().replace('_', ' ');
    }
}
