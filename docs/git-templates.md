# Git Workflow Unified Templates (Optimized for CLI/LLM)

This document contains the standardized Pull Request (PR) and Issue templates. All templates must be populated using the styling, language constraints, and rules specified in [`git-core-rules.md`](./git-core-rules.md).

---

## 📌 1. Pull Request (PR) Templates

### General PR Template
Use for: `Feat` / `Refactor` / `Chore` / `Docs` / `Security` / `Ci` / `Infra`

```markdown
## 개요
- **작업목적**: <!-- Clearly state the intent and purpose of the work. Why was this done? -->
- **핵심 변경점**: <!-- Specify the key change areas and core focus of this work. -->
- **현황 및 문제사항**: <!-- Describe the background context or issues solved. Keep it brief. -->

## 작업내용
<!-- Keep secondary tasks and minor records concise and summarized. Exclude verbose explanations while maintaining clear meaning. -->
- 

## 결과
- 

## 관련이슈
- Closes #
```

### Fix PR Template
Use exclusively for bug fixes and resolutions.

```markdown
## 개요
- **작업목적**: <!-- Clearly state the intent and purpose of the work. Why was this done? -->
- **핵심 변경점**: <!-- Specify the key change areas and core focus of this work. -->
- **현황 및 문제사항**: <!-- Describe the background context or issues solved. Keep it brief. -->
- **에러메시지**: 
  - 
- **원인**: 
  - 

## 작업내용
<!-- Keep secondary tasks and minor records concise and summarized. Exclude verbose explanations while maintaining clear meaning. -->
- 

## 결과
- 

## 관련이슈
- Closes #
```

---

## 📌 2. Issue Template

Use for defining requirements, detailing tasks, or opening a bug-tracking issue before development.

```markdown
## 개요
- **작업목적**: <!-- Clearly state the intent and purpose of the work. Why was this done? -->
- **핵심 변경점**: <!-- Specify the key change areas and core focus of this work. -->
- **현황 및 문제사항**: <!-- Describe the background context or issues solved. Keep it brief. -->

## 작업내용
<!-- Keep secondary tasks and minor records concise and summarized. Exclude verbose explanations while maintaining clear meaning. -->
- 

## 기대효과
- 

## 관련PR
- #
```
