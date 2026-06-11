# Project Instructions

This file contains the foundational mandates, architecture, and conventions for the Finance Portfolio project. All development must strictly adhere to these standards.

## 1. Project Overview
A full-stack finance portfolio and career management application.
- **Backend:** Spring Boot 3.5.9 (Java 21)
- **Frontend:** React 19.2 (Vite 7.3)
- **Infrastructure:** AWS (S3 for file storage, EC2 for hosting), GitHub Actions for CI/CD.

## 2. Architecture & Patterns

### Backend (3-Layer Architecture)
- **Controller Layer:** Handles HTTP requests and responses. Use `@RestController`.
- **Service Layer:** Contains business logic, validation, and transaction management.
- **Domain & Repository Layer:** JPA entities and Spring Data repositories.
- **Data Transfer:** ALWAYS use DTOs for API requests and responses. Never expose entities directly in controllers.
- **Error Handling:** Global exception handling via `GlobalExceptionHandler` returning `ErrorResponse`.

### Frontend
- **API Layer (`src/api`):** Centralized Axios services. Use the `axiosInstance` with interceptors for authentication.
- **State Management:** Use `authStore` for authentication state.
- **Component Structure:** 
  - `src/components`: Shared/reusable UI components.
  - `src/pages`: Page-level components mapped to routes.
  - `src/layout`: Shared layout components (Navbar, Footer).

## 3. Security Conventions
- **Authentication:** JWT-based.
  - **Access Token:** Stored in memory (authStore) and sent via `Authorization: Bearer <token>` header.
  - **Refresh Token:** Stored in HTTP-only cookies.
- **Input Sanitization:** Use **Jsoup** for content that might contain HTML (prevent XSS).
- **Rate Limiting:** Use **Bucket4j** to protect sensitive endpoints (e.g., login).
- **File Validation:** Use **Apache Tika** and **Commons Imaging** for MIME type and pixel-level image validation.

## 4. Coding Standards
- **Naming:**
  - Classes/Interfaces: PascalCase.
  - Methods/Variables: camelCase.
  - Constants: SCREAMING_SNAKE_CASE.
- **Formatting:** Use project-standard IDE formatting.
- **Dependencies:** Verify library usage in `build.gradle` or `package.json` before adding new ones.

## 5. Testing & Quality
- **Backend:** JUnit 5 for unit/integration tests. JaCoCo for coverage reporting.
- **Frontend:** No separate test codes are written.
- **Code Quality:** SonarQube (via SonarCloud) for static analysis and security scanning.

## 6. Workflow
- **CI/CD:** Automated deployments to AWS via GitHub Actions.
- **Environment:** Use `.env` or `application.yml` for configuration. NEVER commit secrets or credentials.
