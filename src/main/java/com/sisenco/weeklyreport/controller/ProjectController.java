package com.sisenco.weeklyreport.controller;

import com.sisenco.weeklyreport.dto.request.AssignUsersRequest;
import com.sisenco.weeklyreport.dto.request.CreateProjectRequest;
import com.sisenco.weeklyreport.dto.request.UpdateProjectRequest;
import com.sisenco.weeklyreport.dto.response.ApiResponse;
import com.sisenco.weeklyreport.dto.response.ProjectMemberResponse;
import com.sisenco.weeklyreport.dto.response.ProjectResponse;
import com.sisenco.weeklyreport.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Project Management.
 *
 * Access rules (enforced via @PreAuthorize):
 *  - ROLE_ADMIN      → full access (create, update, deactivate, assign, remove)
 *  - ROLE_MANAGER    → create, update, assign users, view all
 *  - ROLE_TEAM_MEMBER→ view active projects, view their own projects, view members
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // ─────────────────────────────────────────────
    // CREATE a project  [ADMIN, MANAGER]
    // ─────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Project created successfully"));
    }

    // ─────────────────────────────────────────────
    // GET all projects  [ADMIN, MANAGER]
    // ─────────────────────────────────────────────
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjects() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getAllProjects(), "All projects retrieved"));
    }

    // ─────────────────────────────────────────────
    // GET active projects  [ALL AUTHENTICATED]
    // ─────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getActiveProjects() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getActiveProjects(), "Active projects retrieved"));
    }

    // ─────────────────────────────────────────────
    // GET my projects  [ALL AUTHENTICATED]
    // ─────────────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getMyProjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getMyProjects(userDetails.getUsername()),
                        "Your projects retrieved"));
    }

    // ─────────────────────────────────────────────
    // GET project by ID  [ALL AUTHENTICATED]
    // ─────────────────────────────────────────────
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getProjectById(projectId), "Project retrieved successfully"));
    }

    // ─────────────────────────────────────────────
    // UPDATE project  [ADMIN, MANAGER]
    // ─────────────────────────────────────────────
    @PutMapping("/{projectId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.updateProject(projectId, request), "Project updated successfully"));
    }

    // ─────────────────────────────────────────────
    // DEACTIVATE project  [ADMIN only]
    // ─────────────────────────────────────────────
    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateProject(@PathVariable Long projectId) {
        projectService.deactivateProject(projectId);
        return ResponseEntity.ok(ApiResponse.success(null, "Project deactivated successfully"));
    }

    // ─────────────────────────────────────────────
    // ASSIGN users to project  [ADMIN, MANAGER]
    // ─────────────────────────────────────────────
    @PostMapping("/{projectId}/members")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> assignUsers(
            @PathVariable Long projectId,
            @Valid @RequestBody AssignUsersRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.assignUsersToProject(projectId, request),
                        "Users assigned to project successfully"));
    }

    // ─────────────────────────────────────────────
    // REMOVE user from project  [ADMIN, MANAGER]
    // ─────────────────────────────────────────────
    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeUser(
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        projectService.removeUserFromProject(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User removed from project successfully"));
    }

    // ─────────────────────────────────────────────
    // GET project members  [ALL AUTHENTICATED]
    // ─────────────────────────────────────────────
    @GetMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getProjectMembers(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getProjectMembers(projectId), "Project members retrieved"));
    }
}
