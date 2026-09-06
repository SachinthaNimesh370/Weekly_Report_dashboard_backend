package com.sisenco.weeklyreport.service.impl;

import com.sisenco.weeklyreport.dto.request.CreateUserRequest;
import com.sisenco.weeklyreport.dto.request.UpdateUserRoleRequest;
import com.sisenco.weeklyreport.dto.request.UpdateUserStatusRequest;
import com.sisenco.weeklyreport.dto.response.UserResponse;
import com.sisenco.weeklyreport.entity.Role;
import com.sisenco.weeklyreport.entity.User;
import com.sisenco.weeklyreport.entity.enums.RoleName;
import com.sisenco.weeklyreport.exception.BadRequestException;
import com.sisenco.weeklyreport.exception.DuplicateResourceException;
import com.sisenco.weeklyreport.exception.ResourceNotFoundException;
import com.sisenco.weeklyreport.repository.RoleRepository;
import com.sisenco.weeklyreport.repository.UserRepository;
import com.sisenco.weeklyreport.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request, String currentAdminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (currentAdminEmail != null && currentAdminEmail.equalsIgnoreCase(user.getEmail()) && Boolean.FALSE.equals(request.getIsActive())) {
            throw new BadRequestException("You cannot deactivate your own active account.");
        }

        user.setIsActive(request.getIsActive());
        User updated = userRepository.save(user);
        log.info("User {} status updated to isActive={}", user.getEmail(), user.getIsActive());
        return mapToUserResponse(updated);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request, String currentAdminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (currentAdminEmail != null && currentAdminEmail.equalsIgnoreCase(user.getEmail()) && request.getRole() != RoleName.ROLE_ADMIN) {
            throw new BadRequestException("You cannot remove your own admin privileges.");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseGet(() -> roleRepository.save(Role.builder().name(request.getRole()).build()));

        user.setRole(role);
        User updated = userRepository.save(user);
        log.info("User {} role updated to {}", user.getEmail(), role.getName());
        return mapToUserResponse(updated);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email is already registered: " + normalizedEmail);
        }

        RoleName roleName = request.getRole() != null ? request.getRole() : RoleName.ROLE_TEAM_MEMBER;
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));

        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : "Password@123";

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .department(request.getDepartment() != null ? request.getDepartment().trim() : null)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Admin provisioned new user: {} with role {}", saved.getEmail(), saved.getRole().getName());
        return mapToUserResponse(saved);
    }

    private UserResponse mapToUserResponse(User user) {
        String roleNameStr = "Team Member";
        if (user.getRole() != null) {
            if (user.getRole().getName() == RoleName.ROLE_ADMIN) {
                roleNameStr = "Admin";
            } else if (user.getRole().getName() == RoleName.ROLE_MANAGER) {
                roleNameStr = "Manager";
            }
        }

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .roleName(roleNameStr)
                .department(user.getDepartment())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .projectCount(0)
                .build();
    }
}
