package com.sisenco.weeklyreport.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sisenco.weeklyreport.dto.request.AchievementDto;
import com.sisenco.weeklyreport.dto.request.BlockerDto;
import com.sisenco.weeklyreport.dto.request.HoursBreakdownDto;
import com.sisenco.weeklyreport.dto.request.ReportRequest;
import com.sisenco.weeklyreport.dto.request.ReviewRequest;
import com.sisenco.weeklyreport.dto.request.TaskEntryDto;
import com.sisenco.weeklyreport.dto.response.AchievementResponse;
import com.sisenco.weeklyreport.dto.response.BlockerResponse;
import com.sisenco.weeklyreport.dto.response.HoursBreakdownResponse;
import com.sisenco.weeklyreport.dto.response.PaginatedResponse;
import com.sisenco.weeklyreport.dto.response.ReportResponse;
import com.sisenco.weeklyreport.dto.response.ReportSummaryDto;
import com.sisenco.weeklyreport.dto.response.ReportVersionResponse;
import com.sisenco.weeklyreport.dto.response.ReviewActionResponse;
import com.sisenco.weeklyreport.dto.response.TaskEntryResponse;
import com.sisenco.weeklyreport.entity.Achievement;
import com.sisenco.weeklyreport.entity.Blocker;
import com.sisenco.weeklyreport.entity.HoursBreakdown;
import com.sisenco.weeklyreport.entity.Project;
import com.sisenco.weeklyreport.entity.ReportVersion;
import com.sisenco.weeklyreport.entity.ReviewAction;
import com.sisenco.weeklyreport.entity.TaskEntry;
import com.sisenco.weeklyreport.entity.User;
import com.sisenco.weeklyreport.entity.WeeklyReport;
import com.sisenco.weeklyreport.entity.enums.ReportStatus;
import com.sisenco.weeklyreport.entity.enums.ReviewActionType;
import com.sisenco.weeklyreport.exception.BadRequestException;
import com.sisenco.weeklyreport.exception.DuplicateResourceException;
import com.sisenco.weeklyreport.exception.ResourceNotFoundException;
import com.sisenco.weeklyreport.repository.ProjectRepository;
import com.sisenco.weeklyreport.repository.ReportVersionRepository;
import com.sisenco.weeklyreport.repository.ReviewActionRepository;
import com.sisenco.weeklyreport.repository.UserRepository;
import com.sisenco.weeklyreport.repository.WeeklyReportRepository;
import com.sisenco.weeklyreport.service.ReportService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ReviewActionRepository reviewActionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ─────────────────────────────────────────────
    // CREATE DRAFT REPORT
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ReportResponse createDraftReport(ReportRequest request, String userEmail) {
        log.info("Creating draft weekly report for user {} and week {}", userEmail, request.getWeekStart());

        User user = getUserByEmailOrThrow(userEmail);

        weeklyReportRepository.findByUserAndWeekStart(user, request.getWeekStart()).ifPresent(r -> {
            throw new DuplicateResourceException(
                    "A weekly report for week starting " + request.getWeekStart() + " already exists."
            );
        });

        Project project = getProjectByIdOrThrow(request.getProjectId());
        validateSingleKeyItems(request);

        WeeklyReport report = WeeklyReport.builder()
                .user(user)
                .project(project)
                .weekStart(request.getWeekStart())
                .weekEnd(request.getWeekEnd())
                .status(ReportStatus.DRAFT)
                .tasksPlannedNextWeek(request.getTasksPlannedNextWeek())
                .notes(request.getNotes())
                .currentVersionNo(1)
                .build();

        populateChildEntities(report, request);

        WeeklyReport saved = weeklyReportRepository.save(report);
        log.info("Draft report created with id: {}", saved.getId());
        return toReportResponse(saved);
    }

    // ─────────────────────────────────────────────
    // UPDATE REPORT (DRAFT or NEEDS_CORRECTION only)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ReportResponse updateReport(Long reportId, ReportRequest request, String userEmail) {
        log.info("Updating report id {} by user {}", reportId, userEmail);

        WeeklyReport report = getReportByIdOrThrow(reportId);
        User user = getUserByEmailOrThrow(userEmail);

        // Rule 1 - Ownership Check
        if (!report.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only edit your own weekly reports.");
        }

        // Rule 3 - Editable States Check
        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.NEEDS_CORRECTION) {
            throw new BadRequestException(
                    "Report cannot be edited while in " + report.getStatus() + " status. Only DRAFT and NEEDS_CORRECTION reports can be edited."
            );
        }

        validateSingleKeyItems(request);

        if (!report.getProject().getId().equals(request.getProjectId())) {
            Project newProject = getProjectByIdOrThrow(request.getProjectId());
            report.setProject(newProject);
        }

        report.setWeekStart(request.getWeekStart());
        report.setWeekEnd(request.getWeekEnd());
        report.setTasksPlannedNextWeek(request.getTasksPlannedNextWeek());
        report.setNotes(request.getNotes());

        // Replace child entities
        report.getTaskEntries().clear();
        report.getBlockers().clear();
        report.getAchievements().clear();
        report.getHoursBreakdowns().clear();

        populateChildEntities(report, request);

        WeeklyReport updated = weeklyReportRepository.save(report);
        log.info("Report updated successfully: {}", updated.getId());
        return toReportResponse(updated);
    }

    // ─────────────────────────────────────────────
    // SUBMIT REPORT (Creates Version Snapshot)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ReportResponse submitReport(Long reportId, String userEmail) {
        log.info("Submitting report id {} by user {}", reportId, userEmail);

        WeeklyReport report = getReportByIdOrThrow(reportId);
        User user = getUserByEmailOrThrow(userEmail);

        // Rule 1 - Ownership Check
        if (!report.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only submit your own weekly reports.");
        }

        // Rule 4 - Valid Transitions: DRAFT -> SUBMITTED or NEEDS_CORRECTION -> SUBMITTED
        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.NEEDS_CORRECTION) {
            throw new BadRequestException(
                    "Cannot submit report from status: " + report.getStatus() + ". Only DRAFT and NEEDS_CORRECTION can be submitted."
            );
        }

        // Create immutable version snapshot
        int snapshotVersionNo = report.getCurrentVersionNo();
        String snapshotJson = createReportSnapshotJson(report);

        ReportVersion reportVersion = ReportVersion.builder()
                .report(report)
                .versionNo(snapshotVersionNo)
                .contentJson(snapshotJson)
                .submittedAt(LocalDateTime.now())
                .build();

        report.addReportVersion(reportVersion);
        reportVersionRepository.save(reportVersion);

        // Prepare for next edit cycle if returned for correction
        report.setCurrentVersionNo(snapshotVersionNo + 1);
        report.setStatus(ReportStatus.SUBMITTED);

        WeeklyReport submitted = weeklyReportRepository.save(report);
        log.info("Report {} submitted. Snapshot version {} recorded.", submitted.getId(), snapshotVersionNo);
        return toReportResponse(submitted);
    }

    // ─────────────────────────────────────────────
    // GET REPORT BY ID
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long reportId, String userEmail, boolean isManagerOrAdmin) {
        WeeklyReport report = getReportByIdOrThrow(reportId);
        User user = getUserByEmailOrThrow(userEmail);

        // Team members can only view their own reports
        if (!isManagerOrAdmin && !report.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view another team member's report.");
        }

        return toReportResponse(report);
    }

    // ─────────────────────────────────────────────
    // GET MY REPORTS (PAGINATED)
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ReportSummaryDto> getMyReports(String userEmail, ReportStatus status, int page, int size) {
        User user = getUserByEmailOrThrow(userEmail);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "weekStart"));

        Page<WeeklyReport> reportsPage;
        if (status != null) {
            reportsPage = weeklyReportRepository.findByUserAndStatus(user, status, pageable);
        } else {
            reportsPage = weeklyReportRepository.findByUser(user, pageable);
        }

        Page<ReportSummaryDto> summaryPage = reportsPage.map(this::toReportSummaryDto);
        return PaginatedResponse.fromPage(summaryPage);
    }

    // ─────────────────────────────────────────────
    // GET MANAGER REPORTS (FILTERED & PAGINATED)
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ReportSummaryDto> getManagerReports(
            LocalDate weekStart,
            Long userId,
            Long projectId,
            ReportStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "weekStart", "updatedAt"));

        Specification<WeeklyReport> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (weekStart != null) {
                predicates.add(cb.equal(root.get("weekStart"), weekStart));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<WeeklyReport> reportsPage = weeklyReportRepository.findAll(spec, pageable);
        Page<ReportSummaryDto> summaryPage = reportsPage.map(this::toReportSummaryDto);
        return PaginatedResponse.fromPage(summaryPage);
    }

    // ─────────────────────────────────────────────
    // APPROVE REPORT (MANAGER / ADMIN)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ReportResponse approveReport(Long reportId, String reviewerEmail) {
        log.info("Approving report id {} by reviewer {}", reportId, reviewerEmail);

        WeeklyReport report = getReportByIdOrThrow(reportId);
        User reviewer = getUserByEmailOrThrow(reviewerEmail);

        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new BadRequestException(
                    "Cannot approve report. Only reports in SUBMITTED status can be approved. Current status: " + report.getStatus()
            );
        }

        report.setStatus(ReportStatus.APPROVED);

        // Link review action to latest submitted version
        ReportVersion latestVersion = reportVersionRepository.findByReportOrderByVersionNoDesc(report)
                .stream().findFirst().orElse(null);

        ReviewAction reviewAction = ReviewAction.builder()
                .report(report)
                .reviewer(reviewer)
                .version(latestVersion)
                .action(ReviewActionType.APPROVED)
                .comment("Report approved.")
                .createdAt(LocalDateTime.now())
                .build();

        report.addReviewAction(reviewAction);
        reviewActionRepository.save(reviewAction);

        WeeklyReport approved = weeklyReportRepository.save(report);
        log.info("Report {} approved successfully by {}", reportId, reviewerEmail);
        return toReportResponse(approved);
    }

    // ─────────────────────────────────────────────
    // REQUEST CHANGES (MANAGER / ADMIN)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ReportResponse requestChanges(Long reportId, ReviewRequest reviewRequest, String reviewerEmail) {
        log.info("Requesting changes on report id {} by reviewer {}", reportId, reviewerEmail);

        if (reviewRequest == null || reviewRequest.getComment() == null || reviewRequest.getComment().trim().isEmpty()) {
            throw new BadRequestException("A comment explaining what needs to be changed is required when requesting changes.");
        }

        WeeklyReport report = getReportByIdOrThrow(reportId);
        User reviewer = getUserByEmailOrThrow(reviewerEmail);

        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new BadRequestException(
                    "Cannot request changes. Only reports in SUBMITTED status can be reviewed. Current status: " + report.getStatus()
            );
        }

        report.setStatus(ReportStatus.NEEDS_CORRECTION);

        ReportVersion latestVersion = reportVersionRepository.findByReportOrderByVersionNoDesc(report)
                .stream().findFirst().orElse(null);

        ReviewAction reviewAction = ReviewAction.builder()
                .report(report)
                .reviewer(reviewer)
                .version(latestVersion)
                .action(ReviewActionType.CHANGES_REQUESTED)
                .comment(reviewRequest.getComment().trim())
                .createdAt(LocalDateTime.now())
                .build();

        report.addReviewAction(reviewAction);
        reviewActionRepository.save(reviewAction);

        WeeklyReport updated = weeklyReportRepository.save(report);
        log.info("Changes requested on report {}. Status set to NEEDS_CORRECTION.", reportId);
        return toReportResponse(updated);
    }

    // ─────────────────────────────────────────────
    // GET REPORT VERSIONS
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReportVersionResponse> getReportVersions(Long reportId, String userEmail, boolean isManagerOrAdmin) {
        WeeklyReport report = getReportByIdOrThrow(reportId);
        User user = getUserByEmailOrThrow(userEmail);

        if (!isManagerOrAdmin && !report.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view versions of another team member's report.");
        }

        return reportVersionRepository.findByReportOrderByVersionNoDesc(report)
                .stream()
                .map(v -> ReportVersionResponse.builder()
                        .id(v.getId())
                        .versionNo(v.getVersionNo())
                        .contentJson(v.getContentJson())
                        .submittedAt(v.getSubmittedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────

    private User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private Project getProjectByIdOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private WeeklyReport getReportByIdOrThrow(Long reportId) {
        return weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly report not found with id: " + reportId));
    }

    private void validateSingleKeyItems(ReportRequest request) {
        if (request.getBlockers() != null) {
            long keyIssues = request.getBlockers().stream()
                    .filter(b -> Boolean.TRUE.equals(b.getIsKeyIssue()))
                    .count();
            if (keyIssues > 1) {
                throw new BadRequestException("Only one blocker can be flagged as the key issue for the week.");
            }
        }

        if (request.getAchievements() != null) {
            long keyAchievements = request.getAchievements().stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsKeyAchievement()))
                    .count();
            if (keyAchievements > 1) {
                throw new BadRequestException("Only one achievement can be flagged as the key achievement for the week.");
            }
        }
    }

    private void populateChildEntities(WeeklyReport report, ReportRequest request) {
        if (request.getTaskEntries() != null) {
            for (TaskEntryDto dto : request.getTaskEntries()) {
                TaskEntry task = TaskEntry.builder()
                        .taskName(dto.getTaskName())
                        .priority(dto.getPriority())
                        .plannedPct(dto.getPlannedPct())
                        .actualPct(dto.getActualPct())
                        .status(dto.getStatus())
                        .timePlannedHrs(dto.getTimePlannedHrs())
                        .timeSpentHrs(dto.getTimeSpentHrs())
                        .outputDeliverable(dto.getOutputDeliverable())
                        .build();
                report.addTaskEntry(task);
            }
        }

        if (request.getBlockers() != null) {
            for (BlockerDto dto : request.getBlockers()) {
                Blocker blocker = Blocker.builder()
                        .description(dto.getDescription())
                        .isKeyIssue(Boolean.TRUE.equals(dto.getIsKeyIssue()))
                        .build();
                report.addBlocker(blocker);
            }
        }

        if (request.getAchievements() != null) {
            for (AchievementDto dto : request.getAchievements()) {
                Achievement achievement = Achievement.builder()
                        .description(dto.getDescription())
                        .isKeyAchievement(Boolean.TRUE.equals(dto.getIsKeyAchievement()))
                        .build();
                report.addAchievement(achievement);
            }
        }

        if (request.getHoursBreakdowns() != null) {
            for (HoursBreakdownDto dto : request.getHoursBreakdowns()) {
                HoursBreakdown hours = HoursBreakdown.builder()
                        .taskType(dto.getTaskType())
                        .hours(dto.getHours())
                        .build();
                report.addHoursBreakdown(hours);
            }
        }
    }

    private String createReportSnapshotJson(WeeklyReport report) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("reportId", report.getId());
            snapshot.put("weekStart", report.getWeekStart().toString());
            snapshot.put("weekEnd", report.getWeekEnd().toString());
            snapshot.put("projectId", report.getProject().getId());
            snapshot.put("projectName", report.getProject().getName());
            snapshot.put("tasksPlannedNextWeek", report.getTasksPlannedNextWeek());
            snapshot.put("notes", report.getNotes());

            List<Map<String, Object>> tasks = report.getTaskEntries().stream().map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("taskName", t.getTaskName());
                map.put("priority", t.getPriority().name());
                map.put("plannedPct", t.getPlannedPct());
                map.put("actualPct", t.getActualPct());
                map.put("status", t.getStatus().name());
                map.put("timePlannedHrs", t.getTimePlannedHrs());
                map.put("timeSpentHrs", t.getTimeSpentHrs());
                map.put("outputDeliverable", t.getOutputDeliverable());
                return map;
            }).collect(Collectors.toList());
            snapshot.put("tasks", tasks);

            List<Map<String, Object>> blockers = report.getBlockers().stream().map(b -> {
                Map<String, Object> map = new HashMap<>();
                map.put("description", b.getDescription());
                map.put("isKeyIssue", b.getIsKeyIssue());
                return map;
            }).collect(Collectors.toList());
            snapshot.put("blockers", blockers);

            List<Map<String, Object>> achievements = report.getAchievements().stream().map(a -> {
                Map<String, Object> map = new HashMap<>();
                map.put("description", a.getDescription());
                map.put("isKeyAchievement", a.getIsKeyAchievement());
                return map;
            }).collect(Collectors.toList());
            snapshot.put("achievements", achievements);

            List<Map<String, Object>> hours = report.getHoursBreakdowns().stream().map(h -> {
                Map<String, Object> map = new HashMap<>();
                map.put("taskType", h.getTaskType().name());
                map.put("hours", h.getHours());
                return map;
            }).collect(Collectors.toList());
            snapshot.put("hours", hours);

            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize report snapshot for report {}", report.getId(), e);
            return "{}";
        }
    }

    private ReportResponse toReportResponse(WeeklyReport report) {
        List<TaskEntryResponse> taskList = report.getTaskEntries().stream()
                .map(t -> TaskEntryResponse.builder()
                        .id(t.getId())
                        .taskName(t.getTaskName())
                        .priority(t.getPriority())
                        .plannedPct(t.getPlannedPct())
                        .actualPct(t.getActualPct())
                        .status(t.getStatus())
                        .timePlannedHrs(t.getTimePlannedHrs())
                        .timeSpentHrs(t.getTimeSpentHrs())
                        .outputDeliverable(t.getOutputDeliverable())
                        .build())
                .collect(Collectors.toList());

        List<BlockerResponse> blockerList = report.getBlockers().stream()
                .map(b -> BlockerResponse.builder()
                        .id(b.getId())
                        .description(b.getDescription())
                        .isKeyIssue(b.getIsKeyIssue())
                        .build())
                .collect(Collectors.toList());

        List<AchievementResponse> achievementList = report.getAchievements().stream()
                .map(a -> AchievementResponse.builder()
                        .id(a.getId())
                        .description(a.getDescription())
                        .isKeyAchievement(a.getIsKeyAchievement())
                        .build())
                .collect(Collectors.toList());

        List<HoursBreakdownResponse> hoursList = report.getHoursBreakdowns().stream()
                .map(h -> HoursBreakdownResponse.builder()
                        .id(h.getId())
                        .taskType(h.getTaskType())
                        .hours(h.getHours())
                        .build())
                .collect(Collectors.toList());

        List<ReviewActionResponse> reviewList = report.getReviewActions().stream()
                .map(r -> ReviewActionResponse.builder()
                        .id(r.getId())
                        .reviewerId(r.getReviewer().getId())
                        .reviewerName(r.getReviewer().getFullName())
                        .reviewerEmail(r.getReviewer().getEmail())
                        .againstVersionNo(r.getVersion() != null ? r.getVersion().getVersionNo() : null)
                        .action(r.getAction())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        String latestComment = report.getReviewActions().stream()
                .findFirst() // reviewActions ordered by creation
                .map(ReviewAction::getComment)
                .orElse(null);

        return ReportResponse.builder()
                .id(report.getId())
                .userId(report.getUser().getId())
                .userName(report.getUser().getFullName())
                .userEmail(report.getUser().getEmail())
                .projectId(report.getProject().getId())
                .projectName(report.getProject().getName())
                .weekStart(report.getWeekStart())
                .weekEnd(report.getWeekEnd())
                .status(report.getStatus())
                .tasksPlannedNextWeek(report.getTasksPlannedNextWeek())
                .notes(report.getNotes())
                .currentVersionNo(report.getCurrentVersionNo())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .taskEntries(taskList)
                .blockers(blockerList)
                .achievements(achievementList)
                .hoursBreakdowns(hoursList)
                .reviewActions(reviewList)
                .latestReviewComment(latestComment)
                .build();
    }

    private ReportSummaryDto toReportSummaryDto(WeeklyReport report) {
        String latestComment = report.getReviewActions().stream()
                .findFirst()
                .map(ReviewAction::getComment)
                .orElse(null);

        return ReportSummaryDto.builder()
                .id(report.getId())
                .userId(report.getUser().getId())
                .userName(report.getUser().getFullName())
                .userEmail(report.getUser().getEmail())
                .projectId(report.getProject().getId())
                .projectName(report.getProject().getName())
                .weekStart(report.getWeekStart())
                .weekEnd(report.getWeekEnd())
                .status(report.getStatus())
                .currentVersionNo(report.getCurrentVersionNo())
                .taskCount(report.getTaskEntries().size())
                .blockerCount(report.getBlockers().size())
                .achievementCount(report.getAchievements().size())
                .latestReviewComment(latestComment)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
