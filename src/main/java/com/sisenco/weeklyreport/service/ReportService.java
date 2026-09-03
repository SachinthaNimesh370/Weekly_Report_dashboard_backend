package com.sisenco.weeklyreport.service;

import com.sisenco.weeklyreport.dto.request.ReportRequest;
import com.sisenco.weeklyreport.dto.request.ReviewRequest;
import com.sisenco.weeklyreport.dto.response.PaginatedResponse;
import com.sisenco.weeklyreport.dto.response.ReportResponse;
import com.sisenco.weeklyreport.dto.response.ReportSummaryDto;
import com.sisenco.weeklyreport.dto.response.ReportVersionResponse;
import com.sisenco.weeklyreport.entity.enums.ReportStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for Weekly Report operations.
 * Implemented loosely coupled in service.impl.ReportServiceImpl.
 */
public interface ReportService {

    /**
     * Create a new draft weekly report for a week.
     */
    ReportResponse createDraftReport(ReportRequest request, String userEmail);

    /**
     * Update an existing report (only allowed while status is DRAFT or NEEDS_CORRECTION).
     */
    ReportResponse updateReport(Long reportId, ReportRequest request, String userEmail);

    /**
     * Submit a report for manager review.
     * Transitions status to SUBMITTED and captures an immutable version snapshot.
     */
    ReportResponse submitReport(Long reportId, String userEmail);

    /**
     * Get a report by ID.
     * Team members can only view their own; managers/admins can view any.
     */
    ReportResponse getReportById(Long reportId, String userEmail, boolean isManagerOrAdmin);

    /**
     * Get paginated reports owned by the logged-in team member.
     */
    PaginatedResponse<ReportSummaryDto> getMyReports(String userEmail, ReportStatus status, int page, int size);

    /**
     * Get paginated team reports with filters (for Manager Dashboard).
     */
    PaginatedResponse<ReportSummaryDto> getManagerReports(
            LocalDate weekStart,
            Long userId,
            Long projectId,
            ReportStatus status,
            int page,
            int size
    );

    /**
     * Approve a submitted report (Manager / Admin).
     */
    ReportResponse approveReport(Long reportId, String reviewerEmail);

    /**
     * Request changes on a submitted report with a mandatory comment (Manager / Admin).
     */
    ReportResponse requestChanges(Long reportId, ReviewRequest reviewRequest, String reviewerEmail);

    /**
     * Get all version snapshots for a report.
     */
    List<ReportVersionResponse> getReportVersions(Long reportId, String userEmail, boolean isManagerOrAdmin);
}
