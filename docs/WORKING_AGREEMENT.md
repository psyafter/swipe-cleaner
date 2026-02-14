# Working Agreement

## Purpose

This file is the single canonical source of development rules for this repository.
It exists to keep day-to-day work clear, calm, and consistent for everyone.
If any older document overlaps with this one, this file takes priority.

## Language rules

- All GitHub-facing text must be in English:
  - commit messages
  - pull request titles and descriptions
  - issue comments and review comments
- All code comments must be in English.

## Workflow rules

- Keep changes small and surgical.
- Do not do broad refactors, renames, or style-only rewrites unless explicitly requested.
- Do not retry `git fetch`, `git pull`, or `git rebase` if they fail due to network/proxy errors (for example: `403 CONNECT tunnel failed`). Branch updates should be done via GitHub "Update branch".
- Use Gradle Wrapper behavior only (`./gradlew`). Do not add fallback logic that uses system Gradle.
- GitHub Actions is the source of truth for CI. If local Android SDK/tools are missing, do not claim local builds/tests passed.

## Forbidden files and .gitignore expectations

Never commit build outputs, generated artifacts, secrets, or local diagnostics.

Forbidden artifacts include:

- `**/build/`
- `*.apk`
- `*.aab`
- `*.log`
- `*.hprof`
- `*.tmp`
- `*.jks`
- `*.keystore`
- tokens/API keys/private keys/secrets in any form

Expectations:

- `.gitignore` should cover common generated artifacts and secret file patterns.
- Before creating or updating a PR, verify no forbidden files are staged.

## Binary file policy

- Do not attempt to include binaries via Codex "Create PR" UI.
- If a binary is required (example: `gradle-wrapper.jar`), document exactly how Sasha should add it locally.
- Keep binary-handling instructions explicit in the PR description when applicable.

## UX rule for incomplete functionality

- No dead-end buttons.
- If functionality is not implemented yet, hide or disable the UI control and provide clear user-facing context.

## Pull request template (English)

Use this structure for every PR description:

```md
## Summary
- Short bullet list of what changed.

## Why
- Problem being solved.

## How to test
1. Step-by-step verification flow.
2. Include expected results.
3. Mention limitations/environment assumptions.

## Notes
- Risks, follow-ups, or rollout notes.
```

`How to test` is required in every PR.

## Quick preflight checklist (before Create PR)

- [ ] `git status` reviewed.
- [ ] Changed files reviewed (for example via `git diff --name-only`).
- [ ] No forbidden artifacts or secrets are staged.
- [ ] Commit messages are in English.
- [ ] PR title and description are in English.
- [ ] Any unimplemented UI action is hidden/disabled with clear UX.
- [ ] If local environment is incomplete, CI is referenced as the verification source.

## Good English examples

### Example PR title

- `Fix swipe counter reset when reopening gallery`

### Example PR description

```md
## Summary
- Fix counter initialization when gallery screen is recreated.
- Add a regression test for state restore.

## Why
- Users could see an incorrect swipe count after returning to the gallery.

## How to test
1. Open gallery with at least 10 photos.
2. Swipe 3 photos.
3. Navigate away and return to gallery.
4. Confirm counter still shows 3 swipes.

## Notes
- CI workflow validates test execution in a full Android environment.
```
