# Git Workflow Unified Core Rules (Optimized for CLI/LLM)

This document contains all unified rules for branch naming, commit messages, description constraints, and formatting rules. All Git-related generations must strictly follow this single source of truth.

---

## 📌 1. Unified Work Types

All branch names and commit messages MUST map directly to one of the following 10 standard types:

| Type (Branch) | Type (Commit) | Description |
|---|---|---|
| `feat` | `Feat` | New feature development or implementation |
| `fix` | `Fix` | Bug fixes and error resolutions |
| `refactor` | `Refactor` | Code restructuring without changing business logic or behavior |
| `chore` | `Chore` | Internal build tasks, package manager configuration, or minor resource updates |
| `docs` | `Docs` | Documentation updates (e.g., Markdown files) |
| `test` | `Test` | Adding or modifying test codes |
| `style` | `Style` | Code formatting, missing semicolons (No logic changes) |
| `security` | `Security` | Security vulnerability fixes and security filter application |
| `ci` | `Ci` | CI/CD pipeline automation and script modification (e.g., GitHub Actions workflows) |
| `infra` | `Infra` | Cloud architecture setup and infrastructure configuration (e.g., AWS, Docker) |

---

## 📌 2. Branch Naming Rules

### Format & Length Constraint
- **Format:** `<type>/<short-description>` (e.g., `feat/add-login-flow`)
- Use **lower-kebab-case** for `<short-description>`. Do not use uppercase letters or spaces.
- **Length Constraint (Conciseness):** The number of words in `<short-description>` connected by hyphens (`-`) is optimal at **3–4 words**. A maximum of **5 words** is permitted only when it is difficult to clearly convey the purpose of the work.
- **Issue Number Integration:** If an issue number is provided, place it at the very beginning of the `<short-description>` (e.g., `<type>/12-add-login-flow`).

### Scope-Specific Prefix (Monorepo Rule)
Integrate the appropriate prefix into the `<short-description>` based on the work scope:

| Scope | Prefix | Example |
|---|---|---|
| Backend only | `back-` | `feat/12-back-add-login-api` |
| Frontend only | `front-` | `feat/12-front-login-ui` |
| Infrastructure only | `infra-` | `infra/03-infra-s3-bucket-setup` |
| Cross-functional | (omit) | `feat/12-add-login-flow` |

---

## 📌 3. Commit Message Rules

### Format & Scope-Specific Emoji
- **Format:** `[<Type>] <emoji> <subject>` (e.g., `[Feat] 🍃 로그인 API 구현`)
- Use the correct monorepo scope emoji:

| Scope | Emoji | Example |
|---|---|---|
| Backend only | 🍃 | `[Feat] 🍃 로그인 API 구현` |
| Frontend only | ⚛️ | `[Feat] ⚛️ 로그인 UI 컴포넌트 추가` |
| Infrastructure only | ☁️ | `[Infra] ☁️ S3 버킷 생성 및 환경변수 주입` |
| Cross-functional | (omit) | `[Feat] 포트폴리오 자산 분석 페이지 기능 전체 구현` |

### Subject Constraints
- **Language:** The `<subject>` MUST be written in **Korean** (한국어).
- **Tone/Style:** Use the clear, concise imperative mood (명령조/명사형 마무리: e.g., "~ 구현", "~ 수정", "~ 추가").
- **Length:** Keep the subject line under 20 characters.

---

## 📌 4. Description Formatting & Styling Rules

All Issue and Pull Request descriptions must strictly conform to these rules:

### Shared Constraints (Mandatory)
- **Language:** All generated outputs MUST be written in **Korean** (한국어).
- **Tone:** Use a highly professional, business-like, and engineering-oriented tone. Use **noun-based / command-style endings** (명사형/명령조 마감: e.g., '~부적합', '~적용', '~제거', '~확보'). Do NOT use verbose or conversational endings like '~했습니다' or '~있었습니다'.
- **Terminology:** Actively use rigorous, professional, and standard engineering and architectural terms (e.g., '하이브리드 문서 파이프라인 구축', '동적 데이터 바인딩', '결함 제거', '아키텍처 정렬 유지').
- **Manual Fields:** Leave fields like `관련PR`, `관련이슈`, `closes #`, or `Closes #` in their default template format. Do not hallucinate issue/PR numbers.

### Detail & Compression Rule
- **Purpose-Oriented Context (작업목적 / 핵심 변경점 / 현황 및 문제사항):** Clearly state the "why" (purpose) and the "what" (core changes) to ensure the intent and key focus areas of the work are fully visible, while avoiding excessively long, verbose, or redundant details.
- **Summarized Secondary Tasks & Content:** For simple facts, minor/recording tasks, and secondary details, entirely omit verbose explanations and unnecessary filler. Significantly summarize them into a compressed format, while maintaining enough clarity so the original meaning and context are never lost.

### File-Centric Mapping (PR Only - CRITICAL)
- Group details in the **"작업내용"** section of a PR by **Target File Names (Exclude path)**.
- **Tree-Style Bullet Formatting (`-`):** Organize details into a hierarchical bulleted tree. Do NOT use H3 headings (`###`) for standard files.
  - **Pattern:**
    ```markdown
    - <Scope/Directory/Category>
      - <Logical Category/Topic>
        - `<File Name>`: <Detailed work content in Korean (noun-style endings)>
    ```
- **H3 Header (`###`) Constraint:** Only use `###` for additional notes, structural highlights, or critical emphasis points outside the main tree.

### "결과" Section Guidance (PR Only - CRITICAL)
- Focus on **architectural impact, design values, and structural alignment** using punchy bold headers (e.g., `- **보안강화**: ...`, `- **사용자경험(UX)**: ...`). Avoid simple build success checks.

---

## 🤖 Core Agent Instructions
1. **Keyword-Focused Intuitive Recommendation:** Prioritize the user's provided '작업 목적' (Work Purpose) and '핵심 변경점' (Key Change Points) to intuitively derive highly focused, concise branch names and commit subjects.
2. **Context Isolation:** Refer strictly to the formatting and mapping rules in this file when generating description drafts.
