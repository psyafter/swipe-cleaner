# Working Agreement (Swipe Cleaner)

## 1. Purpose
This document is the single canonical source of truth for development rules in this repository.

A prompt should specify MODE and TASK; everything else is read from this document.

## 2. Language Rules (GitHub English-only)
- GitHub-facing text MUST be in English only:
  - PR title
  - PR description
  - commit messages
  - GitHub comments (issues, reviews, discussions)
- Never use Russian in GitHub artifacts.
- Code comments MUST be in English.

## 3. Workflow Rules (small commits, no refactors, no fetch, CI truth)
- Keep changes small and surgical.
- Do not perform refactors, renames, or style-only rewrites unless explicitly requested by the task.
- If `git fetch` / `git pull` / `git rebase` fails due to network/proxy issues (for example `403 CONNECT tunnel failed`), do NOT retry; Sasha updates branches via GitHub **Update branch**.
- GitHub Actions is authoritative for build/test truth.
- If Android SDK/tools are missing locally, do not claim tests/build passed locally; reference CI logs instead.
- No dead-end UI actions: any visible button must work end-to-end or be disabled/hidden with clear UX.

## 4. Forbidden Files & .gitignore expectations
Never commit build outputs, generated artifacts, secrets, or sensitive credentials.

Forbidden patterns include:
- `**/build/`
- `*.apk`
- `*.aab`
- `*.log`
- `*.hprof`
- `*.tmp`
- `*.jks`
- `*.keystore`
- tokens, API keys, private keys, and secrets in any form

Expectations:
- `.gitignore` should cover the patterns above.
- Before creating a PR, verify that no forbidden files are staged.

## 5. Binary Files Policy (Gradle wrapper jar, icons, etc.)
- Codex “Create PR” UI cannot include binaries.
- If a binary is required (for example `gradle-wrapper.jar`), document exact steps for Sasha to generate and commit it locally.
- Avoid binary changes unless they are explicitly required by the task.

## 6. Preflight Checklist (before Create PR)
- [ ] Scope is minimal and task-focused.
- [ ] No style-only refactors/renames were added.
- [ ] `git status` reviewed.
- [ ] `git diff --name-only` reviewed.
- [ ] No forbidden files or secrets are staged.
- [ ] Commit messages are English-only.
- [ ] PR title and PR description are English-only.
- [ ] If local Android environment is incomplete, verification notes point to GitHub Actions logs.
- [ ] Any unfinished UI action is disabled/hidden with clear UX.

## 7. PR Output Template (English)
Use this template:

```md
## Summary
- What changed.

## Why
- Problem/goal addressed.

## How to test
1. Step-by-step verification.
2. Expected result.
3. Environment limitations (if any).

## Notes
- Risks, follow-ups, and binary-handling instructions (if needed).
```

## 8. Anti-regress guard: user-facing strings
- No hardcoded user-facing strings in Kotlin/Compose.
- Use `strings.xml` with `stringResource(...)` in Compose and `getString(...)` in non-Compose code.
- Verification command:
  - `git grep -nE 'Text\("|title = "|label = "|"[A-Za-z][^"\n]{2,}"' app/src/main/java/com/swipecleaner || true`

## 9. Modes (the key part)
A prompt should specify MODE and TASK; everything else is read from this document.

### MODE: BUGFIX
**Intent**
- Resolve a concrete defect with the smallest safe code change.

**Constraints**
- Fix root cause (or safest minimal correction).
- No broad refactor/rename.
- Preserve current behavior outside the bug scope.

**Steps**
1. Reproduce and define expected behavior.
2. Implement minimal fix.
3. Validate affected flow.
4. Prepare English-only commit(s) and PR text.

**Acceptance Criteria**
- Reported bug is no longer reproducible.
- No unrelated behavior changes.
- Verification steps are documented.

**Deliverables (English PR title+description, changed files list, how to test)**
- PR title in English describing the bug fixed.
- PR description in English using the template from section 7.
- Explicit changed-files list.
- Clear "How to test" steps.

**Default commit plan**
- 1 bug = 1 commit.
- If tests/docs are needed, use at most one additional focused commit.

### MODE: FEATURE
**Intent**
- Add a new user-visible or developer-facing capability with controlled scope.

**Constraints**
- Keep implementation incremental.
- Avoid opportunistic refactors.
- Any exposed UI control must be functional end-to-end or disabled/hidden with clear UX.

**Steps**
1. Define feature slice and non-goals.
2. Implement minimal vertical slice.
3. Add/adjust validation for the new behavior.
4. Document usage/testing and prepare English-only PR artifacts.

**Acceptance Criteria**
- Feature works for intended flow.
- No dead-end UI elements.
- Testing steps and limits are documented.

**Deliverables (English PR title+description, changed files list, how to test)**
- PR title in English naming the feature.
- PR description in English using section 7 template.
- Explicit changed-files list.
- Reproducible "How to test" instructions.

**Default commit plan**
- 1 feature slice = 1–2 commits (implementation, optional tests/docs).
- Keep each commit reviewable and scoped.

### MODE: RELEASE
**Intent**
- Prepare and document a release-ready state with traceable version and verification notes.

**Constraints**
- Release changes only (versioning, release notes, required fixes).
- No unrelated cleanup.
- If binary updates are required, document Sasha-local generation/commit steps.

**Steps**
1. Confirm release scope and version metadata.
2. Apply minimal release changes.
3. Validate via available local checks; use CI as source of truth when local env is limited.
4. Produce English-only PR artifacts with release notes.

**Acceptance Criteria**
- Release metadata is consistent.
- Required release checks are documented with truthful source (local vs CI).
- No forbidden artifacts or secrets are included.

**Deliverables (English PR title+description, changed files list, how to test)**
- PR title in English for release update.
- PR description in English using section 7 template and release notes.
- Explicit changed-files list.
- "How to test" including CI references when applicable.

**Default commit plan**
- 1 release prep = 1 commit.
- If mandatory follow-up docs are needed, add one separate docs commit.
