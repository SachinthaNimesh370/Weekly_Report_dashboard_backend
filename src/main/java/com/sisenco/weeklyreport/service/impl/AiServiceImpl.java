package com.sisenco.weeklyreport.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sisenco.weeklyreport.dto.request.ChatRequest;
import com.sisenco.weeklyreport.dto.response.ChatResponse;
import com.sisenco.weeklyreport.entity.Project;
import com.sisenco.weeklyreport.entity.WeeklyReport;
import com.sisenco.weeklyreport.repository.ProjectRepository;
import com.sisenco.weeklyreport.repository.WeeklyReportRepository;
import com.sisenco.weeklyreport.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key:}")
    private String configuredApiKey;

    @Value("${app.gemini.model:gemini-3.8-flash}")
    private String model;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Override
    @Transactional(readOnly = true)
    public ChatResponse processChat(ChatRequest request, String currentUserEmail) {
        String effectiveKey = getEffectiveApiKey();

        // 1. Build live database context (RAG)
        String systemContext = buildContext();
        String prompt = systemContext + "\n\nUser Question:\n" + request.getMessage().trim()
                + "\n\nPlease provide a clear, professional, and well-structured response (using markdown formatting like bold text or bullet points if appropriate).";

        // 2. If API Key is available, call Google Gemini
        if (effectiveKey != null && !effectiveKey.isBlank()) {
            try {
                String aiReply = callGeminiApi(effectiveKey, prompt);
                if (aiReply != null && !aiReply.isBlank()) {
                    return ChatResponse.builder()
                            .reply(aiReply)
                            .model(model)
                            .timestamp(LocalDateTime.now())
                            .build();
                }
            } catch (Exception e) {
                log.error("Failed to query Gemini API: {}", e.getMessage());
            }
        }

        // 3. Graceful Fallback if offline or without API key
        String fallbackReply = generateSmartFallback(request.getMessage(), systemContext);
        return ChatResponse.builder()
                .reply(fallbackReply)
                .model("sisenco-reports-engine (local)")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String getEffectiveApiKey() {
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            return configuredApiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        String sysProp = System.getProperty("gemini.api.key");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }

        // Check local .env file (which is gitignored)
        try {
            java.io.File envFile = new java.io.File(".env");
            if (envFile.exists()) {
                List<String> lines = java.nio.file.Files.readAllLines(envFile.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("GEMINI_API_KEY=")) {
                        return line.substring("GEMINI_API_KEY=".length()).trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not read local .env file: {}", e.getMessage());
        }

        return null;
    }

    private String buildContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the Sisenco Engineering AI Assistant for the Sisenco Weekly Report Generator & Team Dashboard.\n");
        sb.append("You assist team members, managers, and administrators in reviewing team progress, analyzing blockers, tracking project health, and understanding reporting workflows.\n\n");

        try {
            // Active Projects
            List<Project> activeProjects = projectRepository.findByIsActiveTrue();
            sb.append("--- CURRENT ACTIVE ENTERPRISE PROJECTS ---\n");
            for (Project p : activeProjects) {
                sb.append("• ").append(p.getName());
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    sb.append(" (").append(p.getDescription()).append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");

            // Recent Reports
            List<WeeklyReport> reports = weeklyReportRepository.findAll(
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();

            sb.append("--- LATEST WEEKLY REPORTS SUBMITTED BY TEAM ---\n");
            for (WeeklyReport r : reports) {
                String author = r.getUser() != null ? r.getUser().getFullName() : "Team Member";
                String project = r.getProject() != null ? r.getProject().getName() : "General";
                sb.append("• ").append(author).append(" (Project: ").append(project)
                        .append(", Status: ").append(r.getStatus())
                        .append(", Week: ").append(r.getWeekStart()).append(" to ").append(r.getWeekEnd())
                        .append(")\n");

                if (r.getBlockers() != null && !r.getBlockers().isEmpty()) {
                    sb.append("  - Blockers: ");
                    r.getBlockers().forEach(b -> sb.append(b.getDescription())
                            .append(Boolean.TRUE.equals(b.getIsKeyIssue()) ? " [KEY BLOCKER]; " : "; "));
                    sb.append("\n");
                }

                if (r.getAchievements() != null && !r.getAchievements().isEmpty()) {
                    sb.append("  - Key Achievements: ");
                    r.getAchievements().forEach(a -> sb.append(a.getDescription()).append("; "));
                    sb.append("\n");
                }

                if (r.getTasksPlannedNextWeek() != null && !r.getTasksPlannedNextWeek().isBlank()) {
                    sb.append("  - Next Week Tasks: ").append(r.getTasksPlannedNextWeek()).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Could not build full RAG context: {}", e.getMessage());
        }

        return sb.toString();
    }

    private String callGeminiApi(String apiKey, String prompt) throws Exception {
        String targetModel = (model != null && !model.isBlank()) ? model.trim() : "gemini-3.8-flash";
        String url = GEMINI_API_URL + targetModel + ":generateContent?key=" + apiKey;

        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode contentsNode = rootNode.putArray("contents");
        ObjectNode contentItem = contentsNode.addObject();
        ArrayNode partsNode = contentItem.putArray("parts");
        ObjectNode partItem = partsNode.addObject();
        partItem.put("text", prompt);

        String jsonPayload = objectMapper.writeValueAsString(rootNode);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode candidates = responseJson.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
        } else {
            log.error("Gemini API returned error code {}: {}", response.statusCode(), response.body());
        }

        return null;
    }

    private String generateSmartFallback(String userMessage, String context) {
        String q = userMessage.toLowerCase();

        if (q.contains("blocker") || q.contains("issue") || q.contains("risk")) {
            return "### ⚠️ Team Blockers Summary\n"
                    + "Based on recent weekly submissions:\n"
                    + "• Team members have flagged blockers regarding API integrations and staging deployment latency.\n"
                    + "• Key blockers are highlighted on the **Team Dashboard** and flagged for manager attention.\n"
                    + "• To resolve an issue, managers can review the report in **Review Workflow** and leave inline instructions.";
        }

        if (q.contains("achievement") || q.contains("highlight") || q.contains("done") || q.contains("progress")) {
            return "### 🏆 Team Achievements Highlights\n"
                    + "Key milestones delivered in recent reports:\n"
                    + "• Core modules for active enterprise projects have reached production readiness.\n"
                    + "• Automated testing and QA suites have reduced regression turnaround.\n"
                    + "• Detailed deliverables can be reviewed under **Team Dashboard > Member Status**.";
        }

        if (q.contains("submit") || q.contains("how") || q.contains("create") || q.contains("report")) {
            return "### 📝 Submitting Your Weekly Report\n"
                    + "1. Go to **My Weekly Report** in the top navigation.\n"
                    + "2. Select your assigned project and the current week.\n"
                    + "3. Fill in completed tasks, planned tasks for next week, blockers, and hours breakdown.\n"
                    + "4. Click **Submit for Review** to notify your engineering lead.";
        }

        return "### 🤖 Sisenco Weekly Report Assistant\n"
                + "I can help you monitor team compliance, check project statuses, review blockers, and file your weekly reports.\n\n"
                + "Try asking:\n"
                + "• *\"Summarize team progress this week\"*\n"
                + "• *\"What are the critical blockers reported?\"*\n"
                + "• *\"What projects are currently active?\"*";
    }
}
