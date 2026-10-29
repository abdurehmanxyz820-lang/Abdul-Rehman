# MediSync AI — Connected Healthcare & Medication Adherence Platform

MediSync AI is a secure, cross-platform medication management and home-care agency ecosystem engineered for patients, caregivers, agency administrators, and healthcare systems.

---

## 1. System Architecture Overview

```
                          ┌──────────────────────────┐
                          │   Supabase Cloud / Auth  │
                          │   PostgreSQL + RLS Auth  │
                          └─────────────┬────────────┘
                                        │ (HTTPS / Realtime WSS)
                                        ▼
                  ┌──────────────────────────────────────────┐
                  │       MediSync Data Sync Engine          │
                  │  Offline Mutation Queue + Conflict Mgmt  │
                  └─────────────────────┬────────────────────┘
                                        │
           ┌────────────────────────────┼────────────────────────────┐
           ▼                            ▼                            ▼
┌───────────────────────┐   ┌───────────────────────┐   ┌───────────────────────┐
│ Patient Mobile App    │   │ Caregiver Portal      │   │ Agency Ops Dashboard  │
│ - Daily Schedules     │   │ - Patient Roster      │   │ - 30-Day Adherence    │
│ - Fast One-Tap Intake │   │ - Real-Time Alerts    │   │ - Patient Directory   │
│ - Snooze & Reminders  │   │ - Delegate Dose Log   │   │ - CSV & PDF Reports   │
│ - Emergency Call Hub  │   │ - Clinical Care Notes │   │ - Roster Management   │
└───────────────────────┘   └───────────────────────┘   └───────────────────────┘
                                        │
                                        ▼
                           ┌──────────────────────────┐
                           │ MediSync AI Assistant    │
                           │ Educational Guardrails   │
                           │ Interaction & Food Checks│
                           └──────────────────────────┘
```

---

## 2. Directory Structure

```
.
├── .env.example                               # Environment template for Supabase, AI, and Stripe
├── app/
│   ├── build.gradle.kts                       # Android App module Gradle build definition
│   ├── src/
│       ├── main/
│       │   ├── AndroidManifest.xml            # Declarations, network & notification permissions
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt            # Edge-to-edge Root Activity & role/navigation shell
│       │   │   ├── data/
│       │   │   │   ├── model/Models.kt        # Room Entities, Domain Data Classes, and Enums
│       │   │   │   ├── local/
│       │   │   │   │   ├── MediSyncDao.kt     # Room DAOs for offline queries & sync queue
│       │   │   │   │   └── MediSyncDatabase.kt# SQLite Room Database definition
│       │   │   │   ├── repository/
│       │   │   │   │   └── MediSyncRepository.kt # Offline-first caching, seed data, and sync
│       │   │   │   └── service/
│       │   │   │       ├── AIEducationService.kt  # AI assistant with strict medical guardrails
│       │   │   │       └── ReportExportService.kt # CSV & Clinical PDF report generators
│       │   │   └── ui/
│       │   │       ├── components/CommonComponents.kt # Role switcher, cards, sync pill, alerts
│       │   │       ├── screens/
│       │   │       │   ├── PatientScheduleScreen.kt   # Daily dose tracker, streak, time slot filters
│       │   │       │   ├── MedicationManagementScreen.kt # Active prescriptions, add/archive dialog
│       │   │       │   ├── CaregiverAlertsScreen.kt   # Alert center, roster switcher, care notes
│       │   │       │   ├── AgencyDashboardScreen.kt   # Operational metrics, CSV/PDF export, admit
│       │   │       │   ├── AIAssistantScreen.kt       # Medical safety disclaimers, chat bubbles
│       │   │       │   └── SubscriptionAndSettingsScreen.kt # Tiers, dev payment mode, audit log
│       │   │       ├── theme/
│       │   │       │   ├── Color.kt           # Healthcare blue, healing teal, alert amber
│       │   │       │   └── Theme.kt           # Material Design 3 MediSyncTheme
│       │   │       └── viewmodel/MediSyncViewModel.kt # StateFlows and reactive UI actions
│       │   └── res/                           # Drawables, mipmaps, launcher icons, values
│       └── test/
│           └── java/com/example/ExampleRobolectricTest.kt # JVM Robolectric unit tests
├── supabase/
│   └── migrations/
│       └── 20261029_initial_schema.sql        # Supabase PostgreSQL schema, RLS policies & indexes
├── metadata.json                              # AI Studio project platform identity
└── settings.gradle.kts                        # Root project configuration
```

---

## 3. Database Schema & Security (Supabase PostgreSQL)

The backend schema in `supabase/migrations/20261029_initial_schema.sql` establishes:
1. **Multi-tenant Organizations**: Isolation between healthcare agencies (`organizations`, `organization_members`).
2. **User Profiles & Role-Based Access Control**:
   - `patient`: Can access and update their own dose events and medication schedule.
   - `caregiver`: Can view assigned patients, acknowledge alerts, and log care notes or doses.
   - `agency_admin`: Can manage all patients, caregivers, subscriptions, and export reports for their organization.
   - `system_admin`: Can inspect cross-organization audit streams and system health.
3. **Row Level Security (RLS)**: Enforced directly at the PostgreSQL level via `auth.uid()` checks and helper functions (`auth_user_role()`, `auth_user_org_id()`).
4. **Audit Trail**: Every dose change, role switch, and record admission logs an immutable record to `audit_logs`.

---

## 4. Setup Instructions for Windows 11 & VS Code

### Prerequisites
1. **Java Development Kit (JDK)**: Install JDK 17 or JDK 21 (Temurin or Oracle).
   - Set environment variable `JAVA_HOME` pointing to `C:\Program Files\Eclipse Adoptium\jdk-17...`.
   - Add `%JAVA_HOME%\bin` to your `Path`.
2. **Android SDK**: Install Android Studio or Android Command Line Tools:
   - Set `ANDROID_HOME=C:\Users\<YourUser>\AppData\Local\Android\Sdk`.
   - Add `%ANDROID_HOME%\platform-tools` and `%ANDROID_HOME%\cmdline-tools\latest\bin` to `Path`.
3. **VS Code Extensions**:
   - *Kotlin Language* (by fwcd)
   - *Android iOS Support* or *Android Tools*
   - *Gradle for Java*

### Running on Windows 11 Terminal (PowerShell / Command Prompt)

```powershell
# 1. Clone or extract the project repository
cd medisync-ai

# 2. Copy the environment configuration
copy .env.example .env

# 3. Build debug APK using Gradle
gradle assembleDebug

# 4. Run unit and Robolectric tests
gradle testDebugUnitTest

# 5. Install on connected device or Windows Subsystem for Android (WSA) / Emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Build & Deployment Instructions

### Android Build
- Assemble Debug APK: `gradle :app:assembleDebug`
- Assemble Release APK / Bundle: `gradle :app:bundleRelease`
- Output location: `app/build/outputs/apk/debug/` and `app/build/outputs/bundle/release/`

### Supabase Backend Deployment
1. Log into your Supabase dashboard at [https://supabase.com](https://supabase.com).
2. Create a new project (e.g. `medisync-production`).
3. Navigate to **SQL Editor**.
4. Paste the contents of `supabase/migrations/20261029_initial_schema.sql` and click **Run**.
5. Copy your **Project URL** and **anon public key** into your `.env` file.

### iOS Build Requirements (Flutter / Cross-Platform)
- macOS Sequoia or newer with Xcode 15+ installed.
- CocoaPods (`pod install` in `ios/` folder).
- Apple Developer Team Certificate configured for HealthKit and Push Notifications.

### Web Deployment (Agency Web Dashboard)
- Static host via Firebase Hosting, Vercel, or AWS S3 + CloudFront.
- Environment variables: `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY`.

---

## 6. MediSync AI Educational Assistant

- **Safety Guardrail**: MediSync AI does not provide unverified medical diagnoses or autonomously alter prescriptions.
- **Educational Scope**: Explains drug-food interactions (e.g. Grapefruit with Statins, Metformin with food, Levothyroxine on empty stomach), missed dose protocols (never take double doses), and adherence analytics.
- **Clinical Disclaimer**: Prominently presented on every response and banner.

---

## 7. Subscriptions & Billing

- **Family Care**: $9.99/mo (Up to 2 patients, mobile push & SMS alerts, AI assistant).
- **Home-Care Agency Pro**: $49.99/mo (Up to 50 patients, 20 caregivers, Agency dashboard, CSV/PDF clinical exports).
- **Enterprise Healthcare**: $199.99/mo (Unlimited patients & caregivers, EHR/FHIR integration).
- **Development Mode**: Integrated toggle in the Settings screen simulating webhook verification without charging real credit cards.

---

## 8. Production Readiness Checklist

- [x] Room database schema and migrations configured.
- [x] Row Level Security (RLS) PostgreSQL script prepared.
- [x] Offline-first synchronization queue with conflict resolution.
- [x] Edge-to-edge window insets and accessibility standards met.
- [x] Material 3 clinical healthcare styling with high visual polish.
- [x] Test tags added for automated UI interaction testing.
- [x] Certified CSV and printable clinical PDF export implemented.
- [x] Medical safety disclaimer enforced on AI responses.
- [x] Robolectric and unit tests passing.
