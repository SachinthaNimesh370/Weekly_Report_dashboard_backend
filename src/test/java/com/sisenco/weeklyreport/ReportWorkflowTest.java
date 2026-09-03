package com.sisenco.weeklyreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisenco.weeklyreport.dto.request.*;
import com.sisenco.weeklyreport.entity.Project;
import com.sisenco.weeklyreport.entity.enums.RoleName;
import com.sisenco.weeklyreport.entity.enums.TaskPriority;
import com.sisenco.weeklyreport.entity.enums.TaskStatus;
import com.sisenco.weeklyreport.entity.enums.TaskType;
import com.sisenco.weeklyreport.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    private Long testProjectId;

    @BeforeEach
    void setupProject() {
        Project project = projectRepository.findByNameIgnoreCase("Core Platform")
                .orElseGet(() -> projectRepository.save(Project.builder()
                        .name("Core Platform")
                        .description("Core platform architecture")
                        .isActive(true)
                        .build()));
        testProjectId = project.getId();
    }

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
    @DisplayName("Complete end-to-end report review and correction workflow with RBAC guards")
    void testEndToEndReportWorkflow() throws Exception {
        // 1. Log in member & manager
        String memberToken = loginAndGetToken("member@weeklyreport.com", "Member@123");
        String managerToken = loginAndGetToken("manager@weeklyreport.com", "Manager@123");

        // 2. Member creates a draft weekly report
        LocalDate weekStart = LocalDate.of(2026, 9, 7);
        LocalDate weekEnd = LocalDate.of(2026, 9, 13);

        ReportRequest draftReq = ReportRequest.builder()
                .projectId(testProjectId)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .tasksPlannedNextWeek("Continue implementing analytics dashboard")
                .notes("Frontend integration pending backend review")
                .taskEntries(List.of(
                        TaskEntryDto.builder()
                                .taskName("Build Auth APIs")
                                .priority(TaskPriority.HIGH)
                                .plannedPct(100)
                                .actualPct(100)
                                .status(TaskStatus.DONE)
                                .timePlannedHrs(new BigDecimal("10.0"))
                                .timeSpentHrs(new BigDecimal("8.5"))
                                .outputDeliverable("PR #1 merged")
                                .build()
                ))
                .blockers(List.of(
                        BlockerDto.builder()
                                .description("Slow MySQL queries on local runner")
                                .isKeyIssue(true)
                                .build()
                ))
                .achievements(List.of(
                        AchievementDto.builder()
                                .description("Finished all security filters ahead of schedule")
                                .isKeyAchievement(true)
                                .build()
                ))
                .hoursBreakdowns(List.of(
                        HoursBreakdownDto.builder()
                                .taskType(TaskType.DEVELOPMENT)
                                .hours(new BigDecimal("25.0"))
                                .build(),
                        HoursBreakdownDto.builder()
                                .taskType(TaskType.MEETINGS)
                                .hours(new BigDecimal("5.0"))
                                .build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.taskEntries", hasSize(1)))
                .andExpect(jsonPath("$.data.blockers[0].isKeyIssue", is(true)))
                .andReturn();

        long reportId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Member submits report for manager review
        mockMvc.perform(post("/api/reports/" + reportId + "/submit")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.data.currentVersionNo", is(2)));

        // 4. RBAC Check: Member cannot edit submitted report
        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftReq)))
                .andExpect(status().isBadRequest());

        // 5. RBAC Check: Member cannot access manager endpoints
        mockMvc.perform(get("/api/manager/reports")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        // 6. Manager views team reports
        mockMvc.perform(get("/api/manager/reports")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())));

        // 7. Manager requests changes with a comment
        ReviewRequest requestChangesReq = ReviewRequest.builder()
                .comment("Please add deliverables for task 1 and clarify blocker resolution.")
                .build();

        mockMvc.perform(post("/api/manager/reports/" + reportId + "/request-changes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestChangesReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("NEEDS_CORRECTION")));

        // 8. Member sees status is NEEDS_CORRECTION and edits the report
        draftReq.setNotes("Resolved: Blocker mitigated by MySQL index tuning.");
        mockMvc.perform(put("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes", containsString("Resolved")));

        // 9. Member resubmits report
        mockMvc.perform(post("/api/reports/" + reportId + "/submit")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.data.currentVersionNo", is(3)));

        // 10. Manager reviews and approves
        mockMvc.perform(post("/api/manager/reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        // 11. Check version history (both versions present)
        mockMvc.perform(get("/api/reports/" + reportId + "/versions")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].versionNo", is(2)))
                .andExpect(jsonPath("$.data[1].versionNo", is(1)));
    }

    @Test
    @DisplayName("Should prevent another team member from accessing non-owned report")
    void testOwnershipSecurityRule() throws Exception {
        String member1Token = loginAndGetToken("member@weeklyreport.com", "Member@123");

        // Register second member
        RegisterRequest regReq = RegisterRequest.builder()
                .fullName("David Second")
                .email("david@weeklyreport.com")
                .password("David@123")
                .role(RoleName.ROLE_TEAM_MEMBER)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        String member2Token = loginAndGetToken("david@weeklyreport.com", "David@123");

        // Member 1 creates a report
        ReportRequest req = ReportRequest.builder()
                .projectId(testProjectId)
                .weekStart(LocalDate.of(2026, 9, 21))
                .weekEnd(LocalDate.of(2026, 9, 27))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + member1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        long reportId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Member 2 tries to access Member 1's report -> 403 Forbidden
        mockMvc.perform(get("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + member2Token))
                .andExpect(status().isForbidden());
    }
}
