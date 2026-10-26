# Birr Budget

A native Android expense and budget tracker (Kotlin, Jetpack Compose, Room) with a first-class
**Ethiopian calendar**. Offline-first: all data stays on the phone.

> Status: v0.1.0, first vertical slice. Written without access to an Android SDK, so it has **not
> been compiled yet**. The first GitHub Actions run is the real build test (see below).

## Build it on GitHub (no computer needed)

1. Create a new GitHub repository and upload the contents of this folder (keep the folder
   structure; `.github/workflows/android.yml` must stay at that path).
2. Open the **Actions** tab. The *Android CI* workflow runs on every push to `main`.
3. When it finishes, open the run and download the **birr-budget-debug-apk** artifact.
   Unzip it and install `app-debug.apk` on your phone (allow installs from your browser/files app).
4. If the run fails, copy the red error lines and send them back; usually it is one small file.

The workflow installs Gradle 8.7 itself, so there is no `gradle-wrapper.jar` in the repo. If you
later want a wrapper, run `gradle wrapper --gradle-version 8.7` once on any machine and commit it.

## What works in 0.1.0

- Home: month summary (income, expense, balance) and spending by category
- Transactions: grouped by day, tap to edit or delete, add with the + button
- Budgets: one monthly limit per expense category, with progress and over-budget warning
- Ethiopian **or** Gregorian calendar (Settings), optional Amharic month and day names
- Custom date picker for both calendars (Material's built-in picker is Gregorian-only)
- Ethiopian months (13, including Pagume with 5 or 6 days) drive every monthly view and budget
- Add your own categories, seeded Ethiopian-friendly defaults (Equb / Iddir, Telecom, ...)
- CSV export with both Gregorian and Ethiopian date columns
- Light/dark theme, Material You colours on Android 12+

## Design decisions

- **Dates are stored as Gregorian epoch days.** The Ethiopian calendar is a display and period
  layer (`calendar/`), so switching calendars never changes stored data.
- **Money is stored as integer cents (santim)**, never floating point.
- `EthiopianCalendar` is pure Kotlin (no Android imports) and is covered by unit tests:
  known holidays, Pagume 5/6 days, leap years, and a round trip of every day from 1900 to 2100.
- **Pagume is its own short period.** A "monthly" budget during Pagume covers only 5 or 6 days.
  If you would rather fold Pagume into Nehase for budgets, change `MonthRef` in
  `calendar/Periods.kt` (one place).
- After Gregorian 2100 (not a leap year) Enkutatash moves a day later; the formula handles it.

## Project layout

```
app/src/main/java/app/birrbudget/
  calendar/   EthiopianCalendar.kt (conversion), Periods.kt (MonthRef, DateText)
  data/       Room entities, DAO, database + seed categories, SettingsStore
  ui/         ViewModel, theme, screens, dialogs (incl. the Ethiopian date picker)
  util/       Money (cents <-> text), CsvExport
app/src/test/ ... EthiopianCalendarTest.kt
```

## Not built yet (good next slices)

- Recurring transactions (rent, Equb, salary) and upcoming/overdue list
- Multiple accounts and transfers
- Google Drive backup and restore
- Charts (trends over Ethiopian months/years), home-screen widget
- Edit/delete categories, Amharic UI strings (only dates are Amharic today)
- Import CSV
- Release signing config and Play Store listing

## Licence

The code in this repository was written fresh for this project and does not copy code from
Cashew or any other app. Choose the licence you want and add a `LICENSE` file.
Package name `app.birrbudget` and the app name are placeholders; rename in
`app/build.gradle.kts` (`namespace`, `applicationId`) and `strings.xml` before publishing.
