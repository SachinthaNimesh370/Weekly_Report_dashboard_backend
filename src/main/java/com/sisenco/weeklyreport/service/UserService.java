package com.sisenco.weeklyreport.service;

import com.sisenco.weeklyreport.dto.request.CreateUserRequest;
import com.sisenco.weeklyreport.dto.request.UpdateUserRoleRequest;
import com.sisenco.weeklyreport.dto.request.UpdateUserStatusRequest;
import com.sisenco.weeklyreport.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request, String currentAdminEmail);
    UserResponse updateUserRole(Long id, UpdateUserRoleRequest request, String currentAdminEmail);
    UserResponse createUser(CreateUserRequest request);
}
