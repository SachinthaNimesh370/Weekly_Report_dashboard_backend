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

## 11. Role-Based Access Control

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
| Assign users | ✅ | ✅ | ❌ |
| Remove users | ✅ | ✅ | ❌ |
| View members | ✅ | ✅ | ✅ |

---

## 12. Error Handling

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
| 400 | Validation failure / bad request |
| 401 | Not authenticated / wrong credentials |
| 403 | Role not permitted |
| 404 | Resource not found |
| 409 | Duplicate email or project name |
| 500 | Unexpected server error |

---

## 13. Seed Data

Auto-created on startup by `DataInitializer.java`:

| Email | Password | Role |
|---|---|---|
| `admin@weeklyreport.com` | `Admin@123` | ROLE_ADMIN |
| `manager@weeklyreport.com` | `Manager@123` | ROLE_MANAGER |
| `member@weeklyreport.com` | `Member@123` | ROLE_TEAM_MEMBER |

---

## 14. API Quick Reference

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

── COMING SOON ─────────────────────────────────────────────
Phase 3: /api/reports         Weekly Report CRUD
Phase 4: /api/reports/reviews Manager Review API
Phase 5: /api/dashboard       Statistics & Dashboard
```

---

## 15. Postman Testing Guide

### Postman Environment Variables
| Variable | Value |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `adminToken` | *(paste after login)* |
| `managerToken` | *(paste after login)* |
| `memberToken` | *(paste after login)* |

### Suggested Test Flow
```
1. POST {{baseUrl}}/api/auth/login  (manager)  → save managerToken
2. POST {{baseUrl}}/api/projects               → create project (id=1)
3. POST {{baseUrl}}/api/projects/1/members     → assign user id=3
4. GET  {{baseUrl}}/api/projects/1/members     → verify assignment

5. POST {{baseUrl}}/api/auth/login  (member)   → save memberToken
6. GET  {{baseUrl}}/api/projects/my            → see assigned project
7. POST {{baseUrl}}/api/projects               → expect 403 ✅

8. POST {{baseUrl}}/api/auth/login  (admin)    → save adminToken
9. DELETE {{baseUrl}}/api/projects/1           → deactivate
10. GET {{baseUrl}}/api/projects/all           → admin sees all
```

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
| Phase 3 | Weekly Report CRUD | 🔜 Next |
| Phase 4 | Manager Review API | 🔜 Planned |
| Phase 5 | Dashboard Statistics | 🔜 Planned |

---

*Generated: 2026-09-03 | Developer: Sachinthaya Nimesh | Branch: auth*
