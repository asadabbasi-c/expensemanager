# SmartSpend

SmartSpend is a privacy-first, on-device personal expense tracker for Android. It is **live on the Google Play Store** under the application ID `com.asadabbasi.smartspend`.

All data (expenses, income, goals, recurring transactions, receipt photos) is stored locally on-device using Room. There is no backend server — sync/restore is handled entirely via Android's built-in Auto Backup to Google Drive.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM (ViewModel + StateFlow, Repository pattern)
- **Database:** Room (SQLite), version 4, `fallbackToDestructiveMigration`
- **Navigation:** Jetpack Navigation Compose (bottom nav + nested routes)
- **Charts:** MPAndroidChart
- **OCR:** ML Kit Text Recognition (on-device, no internet)
- **Ads:** Google AdMob (banner + interstitial, free tier only)
- **Billing:** Google Play Billing Client (subscription, Pro tier)
- **Min/Target SDK:** minSdk 26, targetSdk 35, compileSdk 35
- **JVM target:** 17

## Project Structure

```
app/src/main/java/com/example/expensemanager/
├── ExpenseApplication.kt        # App-level DI container (lazy singletons)
├── MainActivity.kt              # Single-activity host, splash screen
├── data/
│   ├── db/                      # Room database + DAOs
│   ├── model/                   # Entities: Expense, Category, Income, SavingGoal, RecurringExpense
│   ├── repository/              # ExpenseRepository - single data access layer
│   ├── service/                 # DataTransferService (export/import)
│   └── settings/                # AppSettings (currency detection/preference)
├── monetization/
│   ├── ProManager.kt            # Pro entitlement state (StateFlow, SharedPreferences)
│   ├── BillingManager.kt        # Google Play Billing integration
│   ├── AdMobBanner.kt           # Banner ad composable
│   ├── InterstitialAdManager.kt # Interstitial ad loading/showing
│   └── ProGate.kt               # Composable wrapper that paywalls a feature
├── receipt/
│   └── ReceiptParser.kt         # Parses OCR text from receipt photos
├── ui/
│   ├── navigation/NavGraph.kt   # All routes + bottom nav
│   ├── screens/                 # One Composable screen per feature
│   └── theme/                   # Theme, colors, LocalCurrency composition local
└── viewmodel/                   # One ViewModel per feature, exposes StateFlow
```

## Core Features

### Expense & Income Tracking
- Manual entry of expenses with amount, category, description, location, date/time
- Categorized spending with custom categories (name, icon, color)
- Income tracking (manual entries)

### Receipt Scanning (ML Kit OCR)
- Capture a receipt photo via camera
- On-device OCR extracts merchant, amount, date
- Photo saved locally (`image_path`), referenced from the expense record

### Voice Expense Entry (Pro)
- Speech-to-text via Android's system speech recognizer (on-device)
- Natural phrases like "Spent 500 at KFC" are parsed into amount/merchant/category

### Recurring Expenses
- Define recurring bills (rent, subscriptions, loans) with frequency: daily/weekly/monthly/yearly
- Tracks `next_due_date`, optional `end_date`, and active/inactive state

### Budgets & Saving Goals (Pro)
- Set a monthly saving goal and income target per month (`yyyy-MM`)
- Dashboard shows progress against the goal

### Dashboard & Charts
- Spending breakdown via MPAndroidChart (category pie/bar charts, trends)

### Export / Import
- Export and import expense/income data via `DataTransferService`

### Multi-Currency
- Currency symbol auto-detected on first launch from SIM/network country, falls back to device locale, then `PKR`
- User can override the detected currency in Settings; choice persisted and applied app-wide via `LocalCurrency`

## Monetization

- **Free tier:** AdMob banner ads (bottom of expense list) + interstitial ads at natural breakpoints
- **Pro tier (subscription):**
  - Product ID: `smartspend_pro`
  - Base plans: `monthly`, `yearly`
  - Gated features: Voice Entry, Budgets & Goals (see `ProGate.kt`)
  - Entitlement managed by `ProManager` (StateFlow-backed, persisted in SharedPreferences) and synced via `BillingManager` (Google Play Billing Client v7)
  - Restore purchases supported

## Data Model (Room entities)

| Entity | Table | Key fields |
|---|---|---|
| `Expense` | `expenses` | amount, categoryId, description, location, date, time, source (manual/sms/voice/receipt), bankName, merchant, imagePath |
| `Category` | `categories` | name, icon, color |
| `Income` | `income` | amount, description, date, time, source, month |
| `SavingGoal` | `saving_goals` | month (PK), goalAmount, incomeTarget |
| `RecurringExpense` | `recurring_expenses` | name, amount, categoryId, frequency, startDate, endDate, nextDueDate, isActive |

## Permissions

- `CAMERA` — receipt photo capture for OCR (local only, never uploaded)
- `RECORD_AUDIO` — voice expense entry via on-device speech recognition
- `AD_ID` — required by AdMob

## Backup

Android Auto Backup to Google Drive is enabled (`allowBackup="true"`, `fullBackupContent`/`dataExtractionRules`), so the local Room database and preferences are backed up automatically with the user's Google account.

## Build & Run

```
./gradlew assembleDebug      # debug build
./gradlew assembleRelease    # release build (minified, ProGuard enabled)
```

Requires:
- Android Studio (Compose-compatible version)
- JDK 17

## Versioning

- Current `versionCode`: 17
- Current `versionName`: 1.7

## Status

Published and live on the Google Play Store.
