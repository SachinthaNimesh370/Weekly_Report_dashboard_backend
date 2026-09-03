# Weekly Report Dashboard — Backend Developer Guide

> **Project**: Weekly Report Generator & Team Dashboard  
> **Backend**: Spring Boot 3.3.5 · Java 17 · MySQL 8 · JWT  
> **Branch**: `auth`  
> **Status**: Phase 1 ✅ Phase 2 ✅ Phase 3 🔜

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Project Setup & Running](#3-project-setup--running)
4. [Architecture](#4-architecture)
5. [Package Structure](#5-package-structure)
6. [Database & ER Diagram](#6-database--er-diagram)
7. [Entities](#7-entities)
8. [Security & JWT](#8-security--jwt)
9. [Phase 1 — Authentication API](#9-phase-1--authentication-api)
10. [Phase 2 — Project Management API](#10-phase-2--project-management-api)
11. [Role-Based Access Control](#11-role-based-access-control)
12. [Error Handling](#12-error-handling)
13. [Seed Data](#13-seed-data)
14. [API Quick Reference](#14-api-quick-reference)
15. [Postman Testing Guide](#15-postman-testing-guide)

---

## 1. Project Overview

The **Weekly Report Dashboard** is a backend REST API system that allows:

- **Team Members** to submit structured weekly reports (tasks, blockers, achievements, hours)
- **Managers** to review, approve, or request corrections to reports
- **Admins** to manage users and projects
- All roles to track weekly productivity via a team dashboard

---

## 2. Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Spring Boot | 3.3.5 | Application framework |
| Spring Security | 6.x | Authentication & Authorization |
| Spring Data JPA | 3.x | ORM & repository layer |
| Hibernate | 6.x | JPA implementation |
| MySQL | 8.x | Primary database |
| H2 (in-memory) | — | Test database |
| JJWT | 0.12.6 | JWT token generation & validation |
| Lombok | Latest | Boilerplate reduction |
| Maven | 3.9.x | Build tool |

---

## 3. Project Setup & Running

### Prerequisites
- Java 17 JDK
- MySQL 8 running on `localhost:3306`
- Maven (or use the bundled `mvnw`)

### Database Configuration

In `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/weekly_report_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: 123123
  jpa:
    hibernate:
      ddl-auto: update

app:
  jwt:
    secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
    expiration-ms: 86400000   # 24 hours
```

> The database `weekly_report_db` is **auto-created** on first run.

### Run the Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run
```

### Run Tests

```bash
./mvnw test
```

> Tests run against an **H2 in-memory** database (no MySQL needed).

### Build JAR

```bash
./mvnw clean package -DskipTests
java -jar target/Weekly_Report_Dashboard_BackEnd-0.0.1-SNAPSHOT.jar
```

---

## 4. Architecture

The project follows a strict **Layered Architecture** with **loose coupling** between layers:

```
┌─────────────────────────────────┐
│         Controller Layer        │  ← REST endpoints, request/response handling
└────────────────┬────────────────┘
                 │ calls (via interface)
┌────────────────▼────────────────┐
│         Service Layer           │
│  ┌──────────────────────────┐   │
│  │   Service Interface      │   │  ← Contract definition (loose coupling)
│  │  (com.../service)        │   │
│  └──────────────────────────┘   │
│  ┌──────────────────────────┐   │
│  │  ServiceImpl Class       │   │  ← Business logic implementation
│  │  (com.../service/impl)   │   │
│  └──────────────────────────┘   │
└────────────────┬────────────────┘
                 │ calls
┌────────────────▼────────────────┐
│        Repository Layer         │  ← Spring Data JPA interfaces
└────────────────┬────────────────┘
                 │ maps to
┌────────────────▼────────────────┐
│          Entity Layer           │  ← JPA entities mapped to MySQL tables
└─────────────────────────────────┘
```

### Key Design Principles

- **Service Interface + ServiceImpl**: Controllers only depend on the interface — never the implementation.
- **Stateless JWT**: No server-side session. Every request must carry a valid Bearer token.
- **RBAC via `@PreAuthorize`**: Role checks enforced at the method level.
- **Global Exception Handling**: All errors returned as uniform JSON via `GlobalExceptionHandler`.
- **DTO Pattern**: Entities are never exposed directly.

---

## 5. Package Structure

```
src/main/java/com/sisenco/weeklyreport/
│
├── WeeklyReportDashboardBackEndApplication.java
│
├── config/
│   ├── ApplicationConfig.java     ← PasswordEncoder, AuthenticationManager
│   ├── CorsConfig.java
│   └── SecurityConfig.java        ← Filter chain, JWT filter, RBAC rules
│
├── controller/
│   ├── AuthController.java        ← /api/auth/**
│   └── ProjectController.java     ← /api/projects/**
│
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── CreateProjectRequest.java
│   │   ├── UpdateProjectRequest.java
│   │   └── AssignUsersRequest.java
│   └── response/
│       ├── ApiResponse.java           ← Generic response wrapper
│       ├── AuthResponse.java
│       ├── UserProfileResponse.java
│       ├── ProjectResponse.java
│       └── ProjectMemberResponse.java
│
├── entity/
│   ├── Role.java
│   ├── User.java
│   ├── Project.java
│   ├── WeeklyReport.java
│   ├── TaskEntry.java
│   ├── Blocker.java
│   ├── Achievement.java
│   ├── HoursBreakdown.java
│   ├── ReportVersion.java
│   ├── ReviewAction.java
│   └── enums/
│       ├── RoleName.java
│       ├── ReportStatus.java
│       ├── TaskPriority.java
│       ├── TaskStatus.java
│       ├── TaskType.java
│       └── ReviewActionType.java
│
├── exception/
│   ├── BadRequestException.java
│   ├── DuplicateResourceException.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
│
├── repository/
│   ├── RoleRepository.java
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   ├── WeeklyReportRepository.java
│   ├── TaskEntryRepository.java
│   ├── BlockerRepository.java
│   ├── AchievementRepository.java
│   ├── HoursBreakdownRepository.java
│   ├── ReportVersionRepository.java
│   └── ReviewActionRepository.java
│
├── security/
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtService.java
│
├── service/
│   ├── AuthService.java           ← Interface
│   ├── ProjectService.java        ← Interface
│   └── impl/
│       ├── AuthServiceImpl.java
│       └── ProjectServiceImpl.java
│
└── util/
    └── DataInitializer.java
```

---

## 6. Database Tables

| Table | Description |
|---|---|
| `roles` | Role lookup |
| `users` | User accounts |
| `user_projects` | Many-to-many join |
| `projects` | Projects |
| `weekly_reports` | Report headers — UNIQUE(user_id, week_start) |
| `task_entries` | Tasks in a report |
| `blockers` | Blockers in a report |
| `achievements` | Achievements in a report |
| `hours_breakdown` | Time breakdown in a report |
| `report_versions` | Versioned snapshots |
| `review_actions` | Manager review decisions |

---

## 7. Entities

### Role
| Column | Type |
|---|---|
| id | BIGINT PK |
| name | `ROLE_ADMIN` / `ROLE_MANAGER` / `ROLE_TEAM_MEMBER` |

### User
| Column | Type |
|---|---|
| id | BIGINT PK |
| full_name | VARCHAR(100) |
| email | VARCHAR(150) UNIQUE |
| password_hash | VARCHAR(255) BCrypt |
| role_id | FK → roles |
| is_active | BOOLEAN |
| created_at / updated_at | DATETIME |

### Project
| Column | Type |
|---|---|
| id | BIGINT PK |
| name | VARCHAR(100) |
| description | VARCHAR(255) |
| is_active | BOOLEAN |
| created_at / updated_at | DATETIME |

### WeeklyReport
| Column | Type |
|---|---|
| id | BIGINT PK |
| user_id | FK → users |
| project_id | FK → projects |
| week_start | DATE |
| week_end | DATE |
| status | DRAFT / SUBMITTED / NEEDS_CORRECTION / APPROVED |
| tasks_planned_next_week | TEXT |
| notes | TEXT |
| current_version_no | INT |

---

## 8. Security & JWT

### Authentication Flow

```
1. POST /api/auth/login  →  validate credentials
2. Generate JWT: {userId, role, fullName, email, exp}
3. Client sends: Authorization: Bearer <token>
4. JwtAuthenticationFilter validates token on every request
5. SecurityContext populated → @PreAuthorize checks role
```

### JWT Payload
```json
{
  "userId": 2,
  "role": "ROLE_MANAGER",
  "fullName": "Lead Manager",
  "sub": "manager@weeklyreport.com",
  "iat": 1788421936,
  "exp": 1788508336
}
```

**Token Expiry**: 24 hours  
**Algorithm**: HMAC-SHA384

---

## 9. Phase 1 — Authentication API

### `AuthService` Interface Methods
```java
AuthResponse register(RegisterRequest request);
AuthResponse login(LoginRequest request);
UserProfileResponse getCurrentUserProfile(String email);
```

| Method | URL | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new user, get JWT |
| POST | `/api/auth/login` | Public | Login, get JWT + role |
| GET | `/api/auth/me` | All authenticated | Get own profile |

#### Register Request
```json
{
  "fullName": "Sachinthaya Nimesh",
  "email": "sachinthaya@sisenco.com",
  "password": "Test@1234",
  "role": "ROLE_TEAM_MEMBER"
}
```

#### Login Request
```json
{
  "email": "manager@weeklyreport.com",
  "password": "Manager@123"
}
```

---

## 10. Phase 2 — Project Management API

### `ProjectService` Interface Methods
```java
ProjectResponse createProject(CreateProjectRequest request);
List<ProjectResponse> getAllProjects();
List<ProjectResponse> getActiveProjects();
ProjectResponse getProjectById(Long projectId);
ProjectResponse updateProject(Long projectId, UpdateProjectRequest request);
void deactivateProject(Long projectId);
ProjectResponse assignUsersToProject(Long projectId, AssignUsersRequest request);
void removeUserFromProject(Long projectId, Long userId);
List<ProjectMemberResponse> getProjectMembers(Long projectId);
List<ProjectResponse> getMyProjects(String email);
```

| Method | URL | Access |
|---|---|---|
| POST | `/api/projects` | ADMIN, MANAGER |
| GET | `/api/projects` | All |
| GET | `/api/projects/all` | ADMIN, MANAGER |
| GET | `/api/projects/my` | All |
| GET | `/api/projects/{id}` | All |
| PUT | `/api/projects/{id}` | ADMIN, MANAGER |
| DELETE | `/api/projects/{id}` | ADMIN only |
| POST | `/api/projects/{id}/members` | ADMIN, MANAGER |
| DELETE | `/api/projects/{id}/members/{userId}` | ADMIN, MANAGER |
| GET | `/api/projects/{id}/members` | All |

---

---

## 11. Phase 3 — Weekly Report Core CRUD & Manager Review Workflow API

### `ReportService` Interface Methods
```java
ReportResponse createDraftReport(ReportRequest request, String userEmail);
ReportResponse updateReport(Long reportId, ReportRequest request, String userEmail);
ReportResponse submitReport(Long reportId, String userEmail);
ReportResponse getReportById(Long reportId, String userEmail, boolean isManagerOrAdmin);
PaginatedResponse<ReportSummaryDto> getMyReports(String userEmail, ReportStatus status, int page, int size);
PaginatedResponse<ReportSummaryDto> getManagerReports(LocalDate weekStart, Long userId, Long projectId, ReportStatus status, int page, int size);
ReportResponse approveReport(Long reportId, String reviewerEmail);
ReportResponse requestChanges(Long reportId, ReviewRequest reviewRequest, String reviewerEmail);
List<ReportVersionResponse> getReportVersions(Long reportId, String userEmail, boolean isManagerOrAdmin);
```

### Report Status State Machine

```
   ┌───────────┐
   │   DRAFT   │
   └─────┬─────┘
         │ submitReport()
         ▼
   ┌───────────┐
   │ SUBMITTED │ ◄───────────────────┐
   └─────┬─────┘                     │
         │                           │
    ┌────┴─────────────────┐         │
    │                      │         │
    │ approve()            │ requestChanges()
    ▼                      ▼         │
┌──────────┐      ┌──────────────────┴┐
│ APPROVED │      │ NEEDS_CORRECTION  │
└──────────┘      └────────┬──────────┘
                           │
                           │ edit() → submitReport()
                           └─────────┘
```

### Endpoints Summary

| Method | URL | Access Role | Description |
|---|---|---|---|
| POST | `/api/reports` | TEAM_MEMBER | Create draft report for a week |
| PUT | `/api/reports/{id}` | Owner (TEAM_MEMBER) | Edit report while in DRAFT or NEEDS_CORRECTION |
| POST | `/api/reports/{id}/submit` | Owner (TEAM_MEMBER) | Submit report for review (creates snapshot version) |
| GET | `/api/reports/{id}` | Owner or MANAGER / ADMIN | Get full report details |
| GET | `/api/reports/my` | TEAM_MEMBER | Get paginated own report history (optional `status`, `page`, `size`) |
| GET | `/api/reports/{id}/versions` | Owner or MANAGER / ADMIN | Get all submission snapshot versions |
| GET | `/api/manager/reports` | MANAGER / ADMIN | Filtered team reports list (`week`, `userId`, `projectId`, `status`, `page`, `size`) |
| POST | `/api/manager/reports/{id}/approve` | MANAGER / ADMIN | Approve submitted report |
| POST | `/api/manager/reports/{id}/request-changes` | MANAGER / ADMIN | Send back for correction with mandatory comment |

### Critical Business Rules Enforced Server-Side

1. **Rule 1 — Ownership**: Team members can only view/edit their own reports. Accessing another member's report returns `403 Forbidden`.
2. **Rule 2 — Manager Read/Review Only**: Managers can view any team report, but cannot edit report content — only approve or request changes via review endpoints.
3. **Rule 3 — Editable States**: A report is strictly editable only when status is `DRAFT` or `NEEDS_CORRECTION`. Once `SUBMITTED` or `APPROVED`, content edits return `400 Bad Request`.
4. **Rule 4 — Valid Transitions**:
   - `DRAFT` → `SUBMITTED`
   - `NEEDS_CORRECTION` → `SUBMITTED`
   - `SUBMITTED` → `APPROVED`
   - `SUBMITTED` → `NEEDS_CORRECTION`
   - Any other transition throws an error.
5. **Rule 5 — Key Issue & Achievement Flags**: Exactly zero or one blocker can be flagged as `isKeyIssue = true`. Exactly zero or one achievement can be flagged as `isKeyAchievement = true`.
6. **Rule 6 — Version Snapshot**: Upon submission, a JSON snapshot of the entire report is stored in `report_versions`, preserving history across correction cycles.

---

## 12. Role-Based Access Control

| Action | ADMIN | MANAGER | TEAM_MEMBER |
|---|:---:|:---:|:---:|
| Register / Login | ✅ | ✅ | ✅ |
| View own profile | ✅ | ✅ | ✅ |
| Create project | ✅ | ✅ | ❌ |
| View active projects | ✅ | ✅ | ✅ |
| View all projects | ✅ | ✅ | ❌ |
| View own projects | ✅ | ✅ | ✅ |
| Update project | ✅ | ✅ | ❌ |
| Deactivate project | ✅ | ❌ | ❌ |
| Assign / Remove users | ✅ | ✅ | ❌ |
| Create draft report | ✅ | ✅ | ✅ |
| Edit own report (draft / correction) | ✅ | ✅ | ✅ |
| Edit other user's report | ❌ | ❌ | ❌ |
| Submit own report | ✅ | ✅ | ✅ |
| View own reports | ✅ | ✅ | ✅ |
| View team reports & dashboard | ✅ | ✅ | ❌ |
| Approve / Request changes | ✅ | ✅ | ❌ |
| View version history | ✅ | ✅ | ✅ (own only) |

---

## 13. Error Handling

All errors return unified JSON:

```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2026-09-03T14:01:00"
}
```

| HTTP | When |
|---|---|
| 400 | Validation failure / illegal state transition |
| 401 | Not authenticated / wrong credentials |
| 403 | Forbidden (trying to access another user's report or manager endpoint) |
| 404 | Resource not found |
| 409 | Duplicate report for same week or duplicate project/email |
| 500 | Unexpected server error |

---

## 14. Seed Data

Auto-created on startup by `DataInitializer.java`:

| Email | Password | Role |
|---|---|---|
| `admin@weeklyreport.com` | `Admin@123` | ROLE_ADMIN |
| `manager@weeklyreport.com` | `Manager@123` | ROLE_MANAGER |
| `member@weeklyreport.com` | `Member@123` | ROLE_TEAM_MEMBER |

---

## 15. API Quick Reference

```
Base URL: http://localhost:8080

── AUTHENTICATION ──────────────────────────────────────────
POST   /api/auth/register
POST   /api/auth/login
GET    /api/auth/me                                  [Auth]

── PROJECT MANAGEMENT ──────────────────────────────────────
POST   /api/projects                                 [Admin, Manager]
GET    /api/projects                                 [Auth]
GET    /api/projects/all                             [Admin, Manager]
GET    /api/projects/my                              [Auth]
GET    /api/projects/{id}                            [Auth]
PUT    /api/projects/{id}                            [Admin, Manager]
DELETE /api/projects/{id}                            [Admin]
POST   /api/projects/{id}/members                    [Admin, Manager]
DELETE /api/projects/{id}/members/{userId}           [Admin, Manager]
GET    /api/projects/{id}/members                    [Auth]

── WEEKLY REPORTS ──────────────────────────────────────────
POST   /api/reports                                  [Member, Manager, Admin]
PUT    /api/reports/{id}                             [Owner]
POST   /api/reports/{id}/submit                      [Owner]
GET    /api/reports/{id}                             [Owner or Manager/Admin]
GET    /api/reports/my?status=&page=&size=           [Member]
GET    /api/reports/{id}/versions                    [Owner or Manager/Admin]

── MANAGER REVIEW WORKFLOW ─────────────────────────────────
GET    /api/manager/reports?week=&userId=&projectId=&status=&page=&size=  [Manager, Admin]
POST   /api/manager/reports/{id}/approve             [Manager, Admin]
POST   /api/manager/reports/{id}/request-changes     [Manager, Admin]

── COMING SOON ─────────────────────────────────────────────
Phase 4: Dashboard Aggregations & Chart Endpoints (/api/manager/dashboard/**)
Phase 5: AI Chat Assistant (/api/ai/chat)
```

---

## 16. Postman Testing Guide — Weekly Reports & Review Workflow

### Sample JSON: Create Draft Weekly Report

`POST http://localhost:8080/api/reports`
Header: `Authorization: Bearer <memberToken>`

```json
{
  "projectId": 1,
  "weekStart": "2026-09-07",
  "weekEnd": "2026-09-13",
  "tasksPlannedNextWeek": "Complete analytics and chart integrations",
  "notes": "Backend API ready for QA testing",
  "taskEntries": [
    {
      "taskName": "Implement Auth & Security Layer",
      "priority": "HIGH",
      "plannedPct": 100,
      "actualPct": 100,
      "status": "DONE",
      "timePlannedHrs": 12.0,
      "timeSpentHrs": 10.5,
      "outputDeliverable": "PR #1 merged with passing tests"
    },
    {
      "taskName": "Weekly Report CRUD APIs",
      "priority": "HIGH",
      "plannedPct": 100,
      "actualPct": 90,
      "status": "IN_PROGRESS",
      "timePlannedHrs": 16.0,
      "timeSpentHrs": 14.0,
      "outputDeliverable": "Draft & submit endpoints tested"
    }
  ],
  "blockers": [
    {
      "description": "Waiting on final UI designs for dashboard charts",
      "isKeyIssue": true
    }
  ],
  "achievements": [
    {
      "description": "Completed full review workflow with automated version snapshotting",
      "isKeyAchievement": true
    }
  ],
  "hoursBreakdowns": [
    {
      "taskType": "DEVELOPMENT",
      "hours": 24.5
    },
    {
      "taskType": "MEETINGS",
      "hours": 4.0
    },
    {
      "taskType": "TESTING",
      "hours": 6.0
    }
  ]
}
```

### Sample Request: Submit Report
`POST http://localhost:8080/api/reports/{id}/submit`
Header: `Authorization: Bearer <memberToken>`

### Sample Request: Manager Request Changes
`POST http://localhost:8080/api/manager/reports/{id}/request-changes`
Header: `Authorization: Bearer <managerToken>`

```json
{
  "comment": "Please provide more details on the testing hours and deliverables."
}
```

### Sample Request: Manager Approve Report
`POST http://localhost:8080/api/manager/reports/{id}/approve`
Header: `Authorization: Bearer <managerToken>`

---

## Development Progress

| Phase | Feature | Status |
|---|---|---|
| Phase 1 | Entities, Enums, Repositories | ✅ Complete |
| Phase 1 | JWT Security & Filters | ✅ Complete |
| Phase 1 | Auth API (register, login, me) | ✅ Complete |
| Phase 2 | Project CRUD API | ✅ Complete |
| Phase 2 | User-Project Assignment | ✅ Complete |
| Phase 2 | RBAC enforcement | ✅ Complete |
| Phase 3 | Weekly Report Core CRUD | ✅ Complete |
| Phase 3 | Review & Correction Workflow | ✅ Complete |
| Phase 3 | Version History & Snapshots | ✅ Complete |
| Phase 4 | Dashboard Analytics & Charts | 🔜 Next |
| Phase 5 | AI Chat Assistant (Optional) | 🔜 Planned |

---

*Generated: 2026-09-03 | Developer: Sachinthaya Nimesh | Branch: reports*

