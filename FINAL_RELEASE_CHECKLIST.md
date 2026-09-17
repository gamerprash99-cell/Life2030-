# LIFE Final Release Checklist

- [x] Complete supplied LifeOS source retained as the base project.
- [x] LIFE controller integrated through existing repositories.
- [x] Existing Room schema retained; no migration added by Phase 18.
- [x] Existing Compose Navigation retained.
- [x] Local voice bridge integrated without cloud fallback.
- [x] No INTERNET permission added by Phase 18.
- [x] No arbitrary external-app or shell control exposed by LIFE.
- [x] Phase 18 documentation added.
- [ ] Android compile/build verification — blocked: no Gradle wrapper/system Gradle in supplied project/environment.
- [ ] Instrumentation/lint verification.
- [ ] Final APK size <= 80 MB verification.
- [ ] Real trained LIFE checkpoint packaging and inference validation.
- [ ] GitHub push only after the remaining release checks are completed.

## 2026-09-18 verification note
- [x] Stitch UI integrated natively in Compose for Profile, Expenses, Add Expense, and App Lock.
- [x] Home Settings action removed; Profile exposes Settings.
- [x] Profile photo persistence added through local DataStore.
- [x] Onboarding startup flash addressed.
- [x] Compose navigation Back handling added.
- [x] Audio capture/playback lifecycle hardened.
- [ ] Full Gradle build/test/lint verification — blocked by missing Gradle wrapper in supplied archive.
