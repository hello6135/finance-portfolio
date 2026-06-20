# Git Workflow Routing Guide (Optimized for CLI/LLM)

This document functions as the core routing controller and output coordinator for all Git-related assistance.
Analyze the user's Korean prompt, identify the corresponding intent, reference the correct child files, and orchestrate the final output format.

---

## 📌 Routing & Orchestration Rules

### Case 1: Branch Name Only
- **User Intent:** Requests for a new branch name based on current work or issue.
- **Reference File:** [`./git-branch-guide.md`](./git-branch-guide.md)

### Case 2: Commit Message Only
- **User Intent:** Requests to draft a commit message for staged changes or work summaries.
- **Reference File:** [`./git-commit-guide.md`](./git-commit-guide.md)

### Case 3: Pull Request Description
- **User Intent:** Requests to generate a PR summary, change logs, or description templates.
- **Reference Files:** 
  1. [`./git-description-guide.md`](./git-description-guide.md) (For common rules, language & content constraints)
  2. [`./git-pr-guide.md`](./git-pr-guide.md) (For the raw PR templates: General vs Fix)

### Case 4: Issue Description
- **User Intent:** Requests to define requirements or create a new issue task before coding.
- **Reference Files:** 
  1. [`./git-description-guide.md`](./git-description-guide.md) (For common rules, language & content constraints)
  2. [`./git-issue-guide.md`](./git-issue-guide.md) (For the raw Issue template)

### Case 5: Branch Name + Commit Message (Simultaneous)
- **User Intent:** Requests for BOTH a branch name AND a commit message in a single prompt.
- **Action:**
  1. Reference `git-branch-guide.md` and generate the branch name first.
  2. Reference `git-commit-guide.md` and generate the commit message second.
  3. Apply the **Cross-Reference Sync Rule** below to ensure consistency.

#### Cross-Reference Sync Rule
Both artifacts MUST be synchronized:
- **Type Matching:** `<type>` in branch == `[Type]` in commit (case difference only: `feat/` → `[Feat]`)
- **Scope Alignment:** Branch prefix → commit emoji mapping is defined in `git-commit-guide.md` Section 2.

### Case 6: Issue & PR Description Integration (Comprehensive)
- **User Intent:** Requests to generate both Issue and PR descriptions simultaneously, or generic description templates.
- **Action:** Reference [`./git-description-guide.md`](./git-description-guide.md), [`./git-issue-guide.md`](./git-issue-guide.md), and [`./git-pr-guide.md`](./git-pr-guide.md). Follow the respective guidelines and templates to generate both filled-out templates.

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
1. **Language Handling:** The user will prompt in Korean. Parse these English guidelines to process the logic.
2. **Context Isolation:** For Case 1 and Case 2, open and read ONLY the `.md` file mapped to the detected case. For Case 3, Case 4, and Case 6, read the combined reference files listed. Do not mix rules from other unmapped files.
3. **Case 5 Priority:** If both a branch name and a commit message are requested, always treat it as Case 5 and apply the Cross-Reference Sync Rule. Do not process them as two separate Case 1 + Case 2 requests.
4. **File Generation Enforcement:** Always adhere strictly to the **Draft Generation & File Output Rule** for any description generation tasks (Cases 3, 4, 5, 6).
