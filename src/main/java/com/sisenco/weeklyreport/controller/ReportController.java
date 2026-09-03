package com.sisenco.weeklyreport.controller;

import com.sisenco.weeklyreport.dto.request.ReportRequest;
import com.sisenco.weeklyreport.dto.response.ApiResponse;
import com.sisenco.weeklyreport.dto.response.PaginatedResponse;
import com.sisenco.weeklyreport.dto.response.ReportResponse;
import com.sisenco.weeklyreport.dto.response.ReportSummaryDto;
import com.sisenco.weeklyreport.dto.response.ReportVersionResponse;
import com.sisenco.weeklyreport.entity.enums.ReportStatus;
import com.sisenco.weeklyreport.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ─────────────────────────────────────────────
    // CREATE DRAFT REPORT [TEAM_MEMBER, ADMIN, MANAGER]
    // ─────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createDraftReport(
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ReportResponse response = reportService.createDraftReport(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Draft weekly report created successfully"));
    }

    // ─────────────────────────────────────────────
    // UPDATE REPORT (DRAFT or NEEDS_CORRECTION only)
    // ─────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ReportResponse response = reportService.updateReport(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Weekly report updated successfully"));
    }

    // ─────────────────────────────────────────────
    // SUBMIT REPORT FOR REVIEW
    // ─────────────────────────────────────────────
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ReportResponse>> submitReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ReportResponse response = reportService.submitReport(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Weekly report submitted for manager review"));
    }

    // ─────────────────────────────────────────────
    // GET REPORT DETAILS (Owner or Manager/Admin)
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        boolean isManagerOrAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"));

        ReportResponse response = reportService.getReportById(id, userDetails.getUsername(), isManagerOrAdmin);
        return ResponseEntity.ok(ApiResponse.success(response, "Report details retrieved"));
    }

    // ─────────────────────────────────────────────
    // GET MY REPORT HISTORY (PAGINATED)
    // ─────────────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReportSummaryDto>>> getMyReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        PaginatedResponse<ReportSummaryDto> response = reportService.getMyReports(userDetails.getUsername(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Your weekly reports retrieved"));
    }

    // ─────────────────────────────────────────────
    // GET REPORT VERSIONS
    // ─────────────────────────────────────────────
    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<ReportVersionResponse>>> getReportVersions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        boolean isManagerOrAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"));

        List<ReportVersionResponse> versions = reportService.getReportVersions(id, userDetails.getUsername(), isManagerOrAdmin);
        return ResponseEntity.ok(ApiResponse.success(versions, "Report versions retrieved"));
    }
}
