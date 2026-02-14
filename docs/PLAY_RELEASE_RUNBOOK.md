# Google Play Release Runbook — Swipe Cleaner

Step-by-step checklist for publishing from first AAB upload to production rollout.

## 0) Preflight

1. Build release AAB locally:
   ```bash
   ./gradlew :app:bundleRelease
   ```
2. Verify version in `app/build.gradle.kts`.
3. Ensure store listing, privacy policy URL/content, and screenshots are ready.

## 1) Internal testing track

1. Open Play Console → Your app → **Testing** → **Internal testing**.
2. Create release.
3. Upload `app-release.aab`.
4. Add release notes.
5. Save and roll out to internal.
6. Add internal testers (emails or Google Group).
7. Share opt-in link and verify install/update.

## 2) Closed testing track

1. Go to **Testing** → **Closed testing**.
2. Create a closed test track.
3. Upload the same or newer AAB.
4. Add testers and publish opt-in URL.
5. Confirm testers can install and make test purchase flow checks.

### Note for new personal Play developer accounts

For some new personal accounts, Google may require:

- at least **12 testers**,
- at least **14 days** of closed testing,
- and a **production access application** submission.

Always check current Play Console prompts for your account status.

## 3) Production access application (if requested)

1. Open the prompted production access form in Play Console.
2. Provide closed testing evidence (testers, duration, feedback summary).
3. Submit application and wait for review decision.

## 4) Production rollout

1. Go to **Release** → **Production**.
2. Create production release.
3. Upload approved AAB.
4. Add “What’s new”.
5. Complete policy forms (Data safety, permissions declarations, content ratings).
6. Review and roll out.

## 5) Post-release checks

1. Install from Play production listing.
2. Verify purchase flow (real SKU active).
3. Verify restore purchases.
4. Monitor Play Console Android vitals and policy alerts.
