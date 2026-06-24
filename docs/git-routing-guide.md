# Git Workflow Routing Guide (Optimized for CLI/LLM)

This document functions as the core routing controller and output coordinator for all Git-related assistance.
Analyze the user's Korean prompt, identify the corresponding intent, reference the correct child files, and orchestrate the final output format.

---

## 🚨 Guardrail Rule (가드레일 - 최우선 반영)

Before processing any Git-related request (branch name, commit message, PR description, issue description, etc.), you MUST verify if the user's prompt explicitly provides both:
1. **"작업의 목적"** (Purpose of the work)
2. **"핵심 변경점(중점사항)"** (Key change points / focus of the work)

If either of these is missing or not clearly specified, you **must immediately halt all processing and execution**. Output EXACTLY the following warning sentence and absolutely nothing else (no greetings, no explanations, no chat markdown/formatting around the text, just the pure text):

작업의 목적과 중점사항을 기입해주시면 더 나은 결과물을 얻을 수 있습니다.

---

## 📌 Routing & Orchestration Rules

### Case 1: Branch Name Only
- **User Intent:** Requests for a new branch name based on current work or issue.
- **Reference File:** [`./git-core-rules.md`](./git-core-rules.md) (Section 2)

### Case 2: Commit Message Only
- **User Intent:** Requests to draft a commit message for staged changes or work summaries.
- **Reference File:** [`./git-core-rules.md`](./git-core-rules.md) (Section 3)

### Case 3: Pull Request Description
- **User Intent:** Requests to generate a PR summary, change logs, or description templates.
- **Reference Files:** 
  1. [`./git-core-rules.md`](./git-core-rules.md) (Section 4)
  2. [`./git-templates.md`](./git-templates.md) (Section 1)

### Case 4: Issue Description
- **User Intent:** Requests to define requirements or create a new issue task before coding.
- **Reference Files:** 
  1. [`./git-core-rules.md`](./git-core-rules.md) (Section 4)
  2. [`./git-templates.md`](./git-templates.md) (Section 2)

### Case 5: Branch Name + Commit Message (Simultaneous)
- **User Intent:** Requests for BOTH a branch name AND a commit message in a single prompt.
- **Action:**
  1. Reference `git-core-rules.md` (Section 2) and generate the branch name first.
  2. Reference `git-core-rules.md` (Section 3) and generate the commit message second.
  3. Apply the **Cross-Reference Sync Rule** below to ensure consistency.

#### Cross-Reference Sync Rule
Both artifacts MUST be synchronized:
- **Type Matching:** `<type>` in branch == `[Type]` in commit (case difference only: `feat/` → `[Feat]`)
- **Scope Alignment:** Branch prefix → commit emoji mapping is defined in `git-core-rules.md` (Section 3).

### Case 6: Issue & PR Description Integration (Comprehensive)
- **User Intent:** Requests to generate both Issue and PR descriptions simultaneously, or generic description templates.
- **Action:** Reference [`./git-core-rules.md`](./git-core-rules.md) and [`./git-templates.md`](./git-templates.md). Follow the respective guidelines and templates to generate both filled-out templates.

---

## 📌 Draft Generation & File Output Rule (CRITICAL)

For cases where description drafts are generated (specifically **Case 3, Case 4, Case 5, and Case 6**):
1. **No Direct Chat Output:** Do NOT output the complete generated markdown content (PR description, Issue template, etc.) directly as the main chat response.
2. **File Creation:** Automatically write the generated markdown content to a new file in the `docs/draft/` directory.
3. **Naming Convention:** Use the exact pattern: `docs/draft/<branch-name>_<YYYYMMDD>.md`
   - *Example:* If the branch name is `docs/add-and-simplify-git-workflow-guides`, and the date is June 20, 2026, the file must be: `docs/draft/docs-add-and-simplify-git-workflow-guides_20260620.md`
   - *Constraint:* Slashes (`/`) in the branch name must be replaced with hyphens (`-`) to form a valid filename.
4. **Ultra-Concise Chat Response (CRITICAL - Token Saving & Readability):**
   - The chat response MUST be extremely brief. Do NOT output, list, or repeat any part of the generated file's written content (e.g., do not print the templates, change lists, or descriptions).
   - The response must strictly contain only:
     - **확인 메시지**: `"요청하신 작업이 <생성된_파일_경로> 파일에 성공적으로 저장되었습니다."`
     - **특이사항**: Any unique conditions, context-driven alerts, or highlights.
     - **규칙 예외 알림**: Notification if the generated output deviates significantly from standard rules.

---

## 🤖 Core Agent Instructions
1. **Guardrail Enforcement (CRITICAL):** First, check if both **"작업의 목적"** and **"핵심 변경점(중점사항)"** are provided. If missing, immediately halt and output only the required warning.
2. **Language Handling:** The user will prompt in Korean. Parse these English guidelines to process the logic.
3. **Context Isolation:** For Case 1 and Case 2, open and read ONLY [`git-core-rules.md`](./git-core-rules.md). For Case 3, Case 4, and Case 6, read both [`git-core-rules.md`](./git-core-rules.md) and [`git-templates.md`](./git-templates.md). Do not mix rules or look for old guide files.
4. **Case 5 Priority:** If both a branch name and a commit message are requested, always treat it as Case 5 and apply the Cross-Reference Sync Rule. Do not process them as two separate Case 1 + Case 2 requests.
5. **Output Guidelines Integration:** Strictly adhere to the unified rules in [`git-core-rules.md`](./git-core-rules.md) when formulating recommendations for branch names, commit messages, and PR descriptions.
6. **File Generation Enforcement:** Always adhere strictly to the **Draft Generation & File Output Rule** for any description generation tasks (Cases 3, 4, 5, 6).
