package com.sisenco.weeklyreport.controller;

import com.sisenco.weeklyreport.dto.request.ReviewRequest;
import com.sisenco.weeklyreport.dto.response.ApiResponse;
import com.sisenco.weeklyreport.dto.response.PaginatedResponse;
import com.sisenco.weeklyreport.dto.response.ReportResponse;
import com.sisenco.weeklyreport.dto.response.ReportSummaryDto;
import com.sisenco.weeklyreport.entity.enums.ReportStatus;
import com.sisenco.weeklyreport.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/manager/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
public class ManagerReviewController {

    private final ReportService reportService;

    // ─────────────────────────────────────────────
    // FILTERED TEAM REPORTS FOR MANAGER DASHBOARD
    // ─────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<ReportSummaryDto>>> getManagerReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PaginatedResponse<ReportSummaryDto> response = reportService.getManagerReports(
                week,
                userId,
                projectId,
                status,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Team reports retrieved"));
    }

    // ─────────────────────────────────────────────
    // APPROVE REPORT
    // ─────────────────────────────────────────────
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ReportResponse>> approveReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ReportResponse response = reportService.approveReport(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Report approved successfully"));
    }

    // ─────────────────────────────────────────────
    // REQUEST CHANGES WITH MANDATORY COMMENT
    // ─────────────────────────────────────────────
    @PostMapping("/{id}/request-changes")
    public ResponseEntity<ApiResponse<ReportResponse>> requestChanges(
            @PathVariable Long id,
            @RequestBody ReviewRequest reviewRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ReportResponse response = reportService.requestChanges(id, reviewRequest, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Changes requested on weekly report"));
    }
}
