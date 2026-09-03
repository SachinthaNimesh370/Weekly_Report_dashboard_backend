package com.sisenco.weeklyreport.service;

import com.sisenco.weeklyreport.dto.request.AssignUsersRequest;
import com.sisenco.weeklyreport.dto.request.CreateProjectRequest;
import com.sisenco.weeklyreport.dto.request.UpdateProjectRequest;
import com.sisenco.weeklyreport.dto.response.ProjectMemberResponse;
import com.sisenco.weeklyreport.dto.response.ProjectResponse;

import java.util.List;

/**
 * Service interface for Project Management operations.
 * Implementations reside in service.impl (loosely coupled).
 */
public interface ProjectService {

    /**
     * Create a new project. (ADMIN / MANAGER only)
     */
    ProjectResponse createProject(CreateProjectRequest request);

    /**
     * Get all projects. (ADMIN & MANAGER: all projects; TEAM_MEMBER: only their assigned projects)
     */
    List<ProjectResponse> getAllProjects();

    /**
     * Get only active projects.
     */
    List<ProjectResponse> getActiveProjects();

    /**
     * Get a single project by ID.
     */
    ProjectResponse getProjectById(Long projectId);

    /**
     * Update a project. (ADMIN / MANAGER only)
     */
    ProjectResponse updateProject(Long projectId, UpdateProjectRequest request);

    /**
     * Deactivate (soft-delete) a project. (ADMIN only)
     */
    void deactivateProject(Long projectId);

    /**
     * Assign one or more users to a project. (ADMIN / MANAGER only)
     */
    ProjectResponse assignUsersToProject(Long projectId, AssignUsersRequest request);

    /**
     * Remove a user from a project. (ADMIN / MANAGER only)
     */
    void removeUserFromProject(Long projectId, Long userId);

    /**
     * Get all members of a project.
     */
    List<ProjectMemberResponse> getProjectMembers(Long projectId);

    /**
     * Get all projects that the currently authenticated user belongs to.
     */
    List<ProjectResponse> getMyProjects(String email);
}
