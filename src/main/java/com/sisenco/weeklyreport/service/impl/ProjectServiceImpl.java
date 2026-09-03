package com.sisenco.weeklyreport.service.impl;

import com.sisenco.weeklyreport.dto.request.AssignUsersRequest;
import com.sisenco.weeklyreport.dto.request.CreateProjectRequest;
import com.sisenco.weeklyreport.dto.request.UpdateProjectRequest;
import com.sisenco.weeklyreport.dto.response.ProjectMemberResponse;
import com.sisenco.weeklyreport.dto.response.ProjectResponse;
import com.sisenco.weeklyreport.entity.Project;
import com.sisenco.weeklyreport.entity.User;
import com.sisenco.weeklyreport.exception.BadRequestException;
import com.sisenco.weeklyreport.exception.DuplicateResourceException;
import com.sisenco.weeklyreport.exception.ResourceNotFoundException;
import com.sisenco.weeklyreport.repository.ProjectRepository;
import com.sisenco.weeklyreport.repository.UserRepository;
import com.sisenco.weeklyreport.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.info("Creating project with name: {}", request.getName());

        projectRepository.findByNameIgnoreCase(request.getName()).ifPresent(p -> {
            throw new DuplicateResourceException("A project with the name '" + request.getName() + "' already exists");
        });

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(true)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Project created with id: {}", saved.getId());
        return toProjectResponse(saved);
    }

    // ─────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::toProjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getActiveProjects() {
        return projectRepository.findByIsActiveTrue()
                .stream()
                .map(this::toProjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId) {
        Project project = findProjectOrThrow(projectId);
        return toProjectResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return user.getProjects()
                .stream()
                .map(this::toProjectResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request) {
        log.info("Updating project id: {}", projectId);
        Project project = findProjectOrThrow(projectId);

        if (request.getName() != null && !request.getName().isBlank()) {
            // Check uniqueness if name is being changed
            if (!request.getName().equalsIgnoreCase(project.getName())) {
                projectRepository.findByNameIgnoreCase(request.getName()).ifPresent(p -> {
                    throw new DuplicateResourceException("A project with the name '" + request.getName() + "' already exists");
                });
            }
            project.setName(request.getName());
        }

        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            project.setIsActive(request.getIsActive());
        }

        return toProjectResponse(projectRepository.save(project));
    }

    // ─────────────────────────────────────────────
    // DEACTIVATE (Soft Delete)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void deactivateProject(Long projectId) {
        log.info("Deactivating project id: {}", projectId);
        Project project = findProjectOrThrow(projectId);
        project.setIsActive(false);
        projectRepository.save(project);
    }

    // ─────────────────────────────────────────────
    // MEMBER MANAGEMENT
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ProjectResponse assignUsersToProject(Long projectId, AssignUsersRequest request) {
        log.info("Assigning {} user(s) to project id: {}", request.getUserIds().size(), projectId);
        Project project = findProjectOrThrow(projectId);

        if (!project.getIsActive()) {
            throw new BadRequestException("Cannot assign users to an inactive project");
        }

        Set<User> usersToAssign = request.getUserIds().stream()
                .map(uid -> userRepository.findById(uid)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + uid)))
                .collect(Collectors.toSet());

        for (User user : usersToAssign) {
            // Add project to user's project set (bidirectional)
            user.getProjects().add(project);
            project.getUsers().add(user);
            userRepository.save(user);
        }

        Project updated = projectRepository.save(project);
        return toProjectResponse(updated);
    }

    @Override
    @Transactional
    public void removeUserFromProject(Long projectId, Long userId) {
        log.info("Removing user id: {} from project id: {}", userId, projectId);
        Project project = findProjectOrThrow(projectId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!project.getUsers().contains(user)) {
            throw new BadRequestException("User with id " + userId + " is not a member of project " + projectId);
        }

        project.getUsers().remove(user);
        user.getProjects().remove(project);

        projectRepository.save(project);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(Long projectId) {
        Project project = findProjectOrThrow(projectId);
        return project.getUsers()
                .stream()
                .map(u -> ProjectMemberResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .role(u.getRole().getName())
                        .isActive(u.getIsActive())
                        .joinedAt(u.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    private ProjectResponse toProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .isActive(project.getIsActive())
                .memberCount(project.getUsers() != null ? project.getUsers().size() : 0)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
