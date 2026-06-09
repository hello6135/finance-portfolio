# Git Workflow Routing Guide (Optimized for CLI/LLM)

This document functions as the core routing controller for all Git-related assistance.
Analyze the user's Korean prompt, identify the corresponding intent, and reference ONLY the specified target file.

---

## 📌 Routing Rules

### Case 1: Branch Name Only
- **User Intent:** Requests for a new branch name based on current work or issue.
- **Reference File:** [`./git-branch-guide.md`](./git-branch-guide.md)

### Case 2: Commit Message Only
- **User Intent:** Requests to draft a commit message for staged changes or work summaries.
- **Reference File:** [`./git-commit-guide.md`](./git-commit-guide.md)

### Case 3: Pull Request Description
- **User Intent:** Requests to generate a PR summary, change logs, or description templates.
- **Reference File:** [`./git-description-guide.md`](./git-description-guide.md)

### Case 4: Issue Description
- **User Intent:** Requests to define requirements or create a new issue task before coding.
- **Reference File:** [`./git-issue-guide.md`](./git-issue-guide.md)

### Case 5: Branch Name + Commit Message (Simultaneous)
- **User Intent:** Requests for BOTH a branch name AND a commit message in a single prompt.
- **Action:**
  1. Reference `git-branch-guide.md` and generate the branch name first.
  2. Reference `git-commit-guide.md` and generate the commit message second.
  3. Apply the **Cross-Reference Sync Rule** below to ensure consistency between the two outputs.

#### Cross-Reference Sync Rule
Both artifacts MUST be synchronized:
- **Type Matching:** `<type>` in branch == `[Type]` in commit (case difference only: `feat/` → `[Feat]`)
- **Scope Alignment:** Branch prefix → commit emoji mapping is defined in `git-commit-guide.md` Section 2.

### Case 6: Issue & PR Description Integration (Comprehensive)
- **User Intent:** Requests to generate both Issue and PR descriptions, or generic description templates.
- **Reference File:** [`./git-description-guide.md`](./git-description-guide.md)

---

## 🤖 Core Agent Instructions
1. **Language Handling:** The user will prompt in Korean. Parse these English guidelines to process the logic.
2. **Context Isolation:** For Cases 1–4, open and read ONLY the `.md` file mapped to the detected case. Do not mix rules from different files.
3. **Case 5 Priority:** If both a branch name and a commit message are requested, always treat it as Case 5 and apply the Cross-Reference Sync Rule. Do not process them as two separate Case 1 + Case 2 requests.
