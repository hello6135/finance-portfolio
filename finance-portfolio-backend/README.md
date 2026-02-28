# portfolio-server (Spring Boot)

Java Spring Boot로 만든 포트폴리오/커리어 관리용 서버 애플리케이션입니다.

## 실행 환경
- Java: 21
- Build: Gradle
- Framework: Spring Boot 3.5.9
- DB: H2 (in-memory)
- View 템플릿: Thymeleaf

## 전체 아키텍처 개요

이 프로젝트는 전형적인 3-Layer 아키텍처를 사용합니다.

- **Controller Layer (`controller`)**: HTTP 요청을 받고, Service를 호출한 뒤 View 또는 JSON 응답을 반환합니다.
- **Service Layer (`service`)**: 비즈니스 로직(검증, 계산, 트랜잭션 등)을 담당합니다.
- **Domain & Repository Layer (`domain`)**: JPA 엔티티와 JPA Repository 인터페이스를 포함합니다.

요청 흐름은 아래와 같습니다.

1. 클라이언트가 HTTP 요청을 보냅니다. (예: `GET /posts`, `GET /api/all`)
2. `controller` 패키지의 Controller가 해당 URL을 매핑하여 요청을 받습니다.
3. Controller는 비즈니스 처리를 위해 `service` 패키지의 Service를 호출합니다.
4. Service는 JPA `Repository`를 사용해 DB에 접근하고, 필요한 계산/검증을 수행합니다.
5. 결과를 Controller로 돌려주고, Controller는 HTML(View) 또는 JSON으로 응답을 생성합니다.

## 폴더 구조

프로젝트 루트 기준 주요 디렉터리 구조는 다음과 같습니다.

```text
src/
  main/
    java/
      com/finance/portfolio_server/
        PortfolioServerApplication.java  # Spring Boot 시작 클래스

        controller/                      # HTTP 요청을 처리하는 컨트롤러 레이어
          FinanceController.java         # 포트폴리오 관련 REST API
          PostController.java            # 게시판(포스트) 관련 MVC 컨트롤러 (Thymeleaf 사용)

        domain/                          # 도메인 모델 및 JPA 리포지토리
          Portfolio.java                 # 포트폴리오 엔티티 (JPA @Entity)
          PortfolioRepository.java       # 포트폴리오 JPA 리포지토리
          Post.java                      # 게시글 엔티티 (제목/내용/작성자/작성시간)
          PostRepository.java            # 게시글 JPA 리포지토리

        service/                         # 비즈니스 로직(Service 레이어)
          PortfolioService.java          # 포트폴리오 저장/통계/위험도 분류 로직
          PostService.java               # 게시글 CRUD 비즈니스 로직

    resources/
      application.properties             # Spring Boot 설정 (DevTools, H2 등)
      static/                            # 정적 리소스 (CSS, JS, 이미지 등)
      templates/
        posts.html                       # Thymeleaf 템플릿 (게시판 화면)
```

## 주요 컴포넌트 설명

### 1. PortfolioServerApplication
- 위치: `com.finance.portfolio_server.PortfolioServerApplication`
- 역할: Spring Boot 애플리케이션의 진입점 (`main` 메서드), 내장 Tomcat을 띄우고 전체 컨텍스트를 초기화합니다.

### 2. Controller Layer (`controller` 패키지)

#### FinanceController
- 어노테이션: `@RestController`
- 주요 엔드포인트:
  - `GET /api/save` : 이름과 주식 비중을 받아 포트폴리오를 저장
  - `GET /api/all`  : 모든 포트폴리오 목록을 JSON으로 반환
  - `GET /api/average` : 전체 사용자의 평균 주식 비중을 계산하여 문자열로 반환
- 특징: JSON 기반 REST API로, 프론트엔드/모바일 앱과 연동하기 좋은 형태입니다.

#### PostController
- 어노테이션: `@Controller`, `@RequestMapping("/posts")`
- 주요 엔드포인트:
  - `GET /posts`        : 게시글 목록을 조회하고 `posts.html` 템플릿을 렌더링 (Model에 `posts` 추가)
  - `POST /posts/save`  : 폼으로부터 전달된 게시글 데이터를 저장 후 `/posts`로 리다이렉트
  - `GET /posts/delete/{id}` : 특정 ID의 게시글을 삭제 후 `/posts`로 리다이렉트
- 특징: 서버사이드 렌더링(MVC) 방식으로, Thymeleaf 템플릿과 함께 동작합니다.

### 3. Service Layer (`service` 패키지)

#### PortfolioService
- 역할: 포트폴리오 관련 비즈니스 로직 담당
- 주요 기능:
  - 포트폴리오 저장 시 주식/채권 비중 계산 (`bondWeight = 1.0 - stockWeight`)
  - 주식 비중에 따라 위험도 자동 분류 (예: 70% 이상이면 "공격형")
  - 전체 포트폴리오의 평균 주식 비중 계산 및 메시지 생성

#### PostService
- 역할: 게시글(Post) 관련 비즈니스 로직 담당
- 주요 기능:
  - 게시글 목록 조회 (`getAllPosts`)
  - 게시글 저장 시 생성 시간 설정 등 추가 로직 처리 (`savePost`)
  - 게시글 삭제 (`deletePost`)

### 4. Domain & Repository Layer (`domain` 패키지)

#### 엔티티 (Entities)
- `Portfolio`
  - 필드: `id`, `name`, `stockWeight`, `bondWeight`, `riskType`
  - 어노테이션: `@Entity`, `@Getter`, `@Setter`

- `Post`
  - 필드: `id`, `title`, `content`, `author`, `createdAt`
  - 어노테이션: `@Entity`, `@Getter`, `@Setter`

#### 리포지토리 (Repositories)
- `PortfolioRepository`
  - `JpaRepository<Portfolio, Long>` 상속
  - 기본 CRUD 메서드 자동 제공 (`findAll`, `save`, `deleteById` 등)

- `PostRepository`
  - `JpaRepository<Post, Long>` 상속
  - 게시글 CRUD를 위한 기본 메서드 제공

## 실행 방법

### 1) Gradle로 실행 (권장)
```bash
./gradlew bootRun      # macOS / Linux
.\gradlew bootRun      # Windows PowerShell / CMD
```

### 2) IDE에서 실행
- IntelliJ IDEA 또는 VS Code/Cursor에서
  - `PortfolioServerApplication` 클래스 우클릭 → `Run` 또는 `Debug`

## 주요 URL 정리

- 게시판(MVC)
  - `GET http://localhost:8080/posts`       : 게시판 목록 페이지 (Thymeleaf 렌더링)
  - `POST http://localhost:8080/posts/save` : 게시글 저장
  - `GET http://localhost:8080/posts/delete/{id}` : 게시글 삭제

- 포트폴리오 REST API
  - `GET http://localhost:8080/api/save?name=홍길동&stock=0.7`
  - `GET http://localhost:8080/api/all`
  - `GET http://localhost:8080/api/average`

## 개발 모드(DevTools) 관련

- `build.gradle`에 `spring-boot-devtools`가 포함되어 있어, 코드 변경 후 자동 재시작이 동작하도록 구성되어 있습니다.
- `application.properties`에 DevTools 관련 옵션이 설정되어 있어, 저장 시 빠르게 서버가 재시작되고 변경 내용이 반영됩니다.


