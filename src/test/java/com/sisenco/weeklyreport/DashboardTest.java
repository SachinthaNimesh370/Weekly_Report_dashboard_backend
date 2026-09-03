package com.sisenco.weeklyreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisenco.weeklyreport.dto.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest loginReq = LoginRequest.builder().email(email).password(password).build();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).path("data").path("token").asText();
    }

    @Test
    @DisplayName("Manager can access dashboard summary metrics")
    void testDashboardSummary() throws Exception {
        String managerToken = loginAndGetToken("manager@weeklyreport.com", "Manager@123");

        mockMvc.perform(get("/api/manager/dashboard/summary")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.weekStart", notNullValue()))
                .andExpect(jsonPath("$.data.complianceRate", notNullValue()))
                .andExpect(jsonPath("$.data.notStartedCount", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("Manager can access member submission status including derived NOT_STARTED")
    void testMemberStatus() throws Exception {
        String managerToken = loginAndGetToken("manager@weeklyreport.com", "Manager@123");

        mockMvc.perform(get("/api/manager/dashboard/status")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].status", notNullValue()));
    }

    @Test
    @DisplayName("Manager can access chart analytics endpoints (tasks trend, projects workload, time distribution)")
    void testAnalyticsCharts() throws Exception {
        String managerToken = loginAndGetToken("manager@weeklyreport.com", "Manager@123");

        // 1. Tasks trend
        mockMvc.perform(get("/api/manager/dashboard/tasks-trend?weeks=4")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(4)));

        // 2. Project workload
        mockMvc.perform(get("/api/manager/dashboard/projects")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));

        // 3. Time distribution
        mockMvc.perform(get("/api/manager/dashboard/time-distribution")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", not(empty())));

        // 4. Activity feed
        mockMvc.perform(get("/api/manager/dashboard/activity?limit=5")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("Team member cannot access manager dashboard endpoints (403 Forbidden)")
    void testTeamMemberForbiddenFromDashboard() throws Exception {
        String memberToken = loginAndGetToken("member@weeklyreport.com", "Member@123");

        mockMvc.perform(get("/api/manager/dashboard/summary")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/manager/dashboard/status")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/manager/dashboard/tasks-trend")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }
}
