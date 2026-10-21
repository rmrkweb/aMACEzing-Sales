# aMACEzing Sales

Sales monitoring, quotation and inventory app for **aMACEzing Printing Services**, built for two connected businesses:

- **Laser Engraving**
- **Sintra Board Printing**

It is a native Android app (Kotlin + Jetpack Compose) that works **offline first** and backs everything up to **Firebase Cloud Firestore** under the owner's Google account.

---

## Table of contents

1. [Features](#features)
2. [Screens and navigation](#screens-and-navigation)
3. [Tech stack](#tech-stack)
4. [Project structure](#project-structure)
5. [Getting started](#getting-started)
6. [Firebase setup](#firebase-setup)
7. [Data and sync](#data-and-sync)
8. [Security](#security)
9. [PDF documents and branding](#pdf-documents-and-branding)
10. [Importing and exporting data](#importing-and-exporting-data)
11. [CI/CD and releases](#cicd-and-releases)
12. [In-app updates](#in-app-updates)
13. [Testing](#testing)
14. [Troubleshooting](#troubleshooting)
15. [Contributing](#contributing)

---

## Features

**Sales and orders**
- Record sales orders with multiple product lines, quantities, editable unit prices and per-line subtotals.
- Adding the same product twice merges into one line instead of creating a duplicate.
- Percentage or fixed-amount discounts, with gross sales, net sales and estimated profit calculated automatically.
- Payments: record partial or full payments, with payment status tracking (Pending, Partially Paid, Paid, Cancelled) and outstanding balances.
- Duplicate, edit, delete and search orders, and filter by business and payment status.
- Order numbers use a configurable prefix per business (for example `ENG-` and `SNT-`).

**Quotations**
- Create price quotations with line items, validity dates and a status (Draft, Sent, Accepted, Declined, Converted).
- Generate branded **A4 PDF** quotations and receipts, and convert an accepted quotation into a sales order.

**Dashboard and reports**
- Net sales, estimated profit, collected cash, outstanding receivables, gross sales, discounts and average order value.
- Sales and profit trend chart, business comparison, product sales breakdown, and paid vs unpaid chart.
- Outstanding receivables list with one-tap payment recording.
- **Profit masking** to hide profit figures when showing the screen to others.

**Catalog and customers**
- Product catalog with selling price, unit cost and per-unit margin.
- Historical price isolation: editing a price only affects future orders, because past orders keep their own price and cost snapshot.
- Customer list with contact details.

**Account and security**
- Google sign-in (Firebase Authentication with Credential Manager).
- Optional **biometric app lock** (fingerprint, face or device PIN).
- Logout backs up to the cloud, wipes local data, and returns to Google sign-in. Data is restored on the next sign-in.

**Business branding**
- Business profile with name, tagline, owner, contact details, website, TIN, logo, footers, payment instructions and quotation terms.
- The custom logo appears on generated PDFs, and the app logo is used as a fallback.

**Data tools**
- Excel/CSV migration with preview and validation, plus CSV export of transactions.

**Updates**
- In-app updater that checks the latest GitHub Release and installs the new APK.

---

## Screens and navigation

Bottom navigation:

| Tab | Screen | Purpose |
| --- | --- | --- |
| Home | Dashboard | KPIs, charts, outstanding receivables, recent sales |
| Orders | Quotations | Create and manage price quotations |
| Sales | Transactions | Sales orders, payments, receipts |
| Inventory | Products | Catalog and pricing |
| Settings | Settings | Business names, order prefixes, profit calculation, profit masking |

Tap the profile photo (top right) to open **Profile**, which also acts as the hub for:

- Business branding and document settings
- **App Lock** toggle
- Shortcuts to **Sales Reports**, **Data Import** and **Customers**
- **Log out**

---

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose, Material 3 (Compose BOM 2024.09.00) |
| Architecture | MVVM with a single `SalesViewModel`, Kotlin Coroutines and `StateFlow` |
| Local database | Room 2.7.0 (KSP) |
| Cloud | Firebase Cloud Firestore, Firebase Authentication (BOM 34.17.0) |
| Sign-in | AndroidX Credentials and Google ID |
| Images | Coil 2.7.0 |
| Security | AndroidX Biometric |
| Build | Gradle 9.3.1, Android Gradle Plugin 9.1.1 |
| Android | `minSdk` 24, `targetSdk` and `compileSdk` 36, application ID `com.amacezing.sales` |
| Tests | JUnit, Robolectric, Firestore emulator rules tests |

---

## Project structure

```
.
├── .github/workflows/
│   ├── android-build.yml        # Debug APK build and unit tests on every push / PR
│   └── android-release.yml      # Manual release build, GitHub Release publishing
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/amacezing/sales/
│       │   │   ├── MainActivity.kt          # App root, scaffold, splash, app lock gate
│       │   │   ├── SalesApplication.kt
│       │   │   ├── data/                    # Room database, DAOs, SalesRepository
│       │   │   │   ├── firebase/            # Firestore models, mappers, error handling
│       │   │   │   └── repository/firestore # One Firestore repository per collection
│       │   │   ├── model/                   # Room entities and enums
│       │   │   ├── pdf/                     # PDF generation (quotations, receipts)
│       │   │   ├── security/                # App lock and biometric helpers
│       │   │   ├── update/                  # In-app updater (GitHub Releases)
│       │   │   ├── ui/
│       │   │   │   ├── auth/                # Google sign-in screen
│       │   │   │   ├── components/          # Dialogs, cards, charts, splash screen
│       │   │   │   ├── navigation/          # Screen enum
│       │   │   │   ├── screens/             # Feature screens
│       │   │   │   └── theme/
│       │   │   ├── util/                    # Formatters, calculator, CSV, logo codec
│       │   │   └── viewmodel/               # SalesViewModel
│       │   └── res/
│       └── test/                            # Unit, Robolectric and rules tests
├── gradle/
│   ├── libs.versions.toml                   # Version catalog
│   └── wrapper/
├── firebase.json                            # Firestore database and emulator config
├── firestore.rules                          # Security rules
├── firestore.test.js                        # Rules tests (emulator)
├── BUILD_AND_RELEASE.md                     # Detailed build and release guide
└── BRANDING_CHANGES.md
```

---

## Getting started

### Prerequisites

- Android Studio (current stable) or the command-line Android SDK
- **JDK 21**
- A Firebase project (see [Firebase setup](#firebase-setup))
- An Android device or emulator running Android 7.0 (API 24) or newer

### Clone and build

```bash
git clone https://github.com/<owner>/aMACEzing-Sales.git
cd aMACEzing-Sales

# Place your Firebase config at app/google-services.json (see below), then:
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

### Run unit tests

```bash
./gradlew :app:testDebugUnitTest
```

### Local environment file

`.env.example` is provided as a template. Copy it to `.env` if you need local environment variables. The app does not require any API key to build.

---

## Firebase setup

1. **Create a Firebase project** and add an Android app with the package name `com.amacezing.sales`.
2. **Enable Authentication**, then turn on the **Google** provider.
3. **Add your SHA-1 and SHA-256 fingerprints** to the Android app in Firebase (Project settings). Google sign-in will fail without the SHA-1 of the keystore that signs your APK. Use a stable keystore, otherwise this changes on every build (see [Signing](#signing)).
4. **Download `google-services.json`** and place it at `app/google-services.json`. It is gitignored for CI, where it comes from a secret.
5. **Create the Firestore database.**
   - The app uses a named database, set in two places that must match:
     - `app/src/main/res/values/firebase_applet_config.xml` → `firestore_database_id`
     - `firebase.json` → `firestore[0].database`
   - If you use the standard database, set both to `(default)`.
6. **Deploy the security rules** to that same database:
   ```bash
   firebase deploy --only firestore:rules
   ```
   Or paste `firestore.rules` into Firestore → Rules and publish. Rules that are out of date are the most common cause of `PERMISSION_DENIED` errors.

### Firestore data model

Everything is stored per user, under `users/{uid}`:

| Path | Contents |
| --- | --- |
| `users/{uid}` | User profile |
| `users/{uid}/businesses/*` | Business definitions |
| `users/{uid}/products/*` | Catalog |
| `users/{uid}/customers/*` | Customers |
| `users/{uid}/transactions/*` | Sales orders, including embedded order lines and payments |
| `users/{uid}/settings/*` | App settings |
| `users/{uid}/business_profile/*` | Branding profile, including the logo (as a compressed image) |

Quotations are **not** synced to the cloud. They are stored on the device only.

---

## Data and sync

- **Offline first:** Room is the working copy. Every save succeeds locally, then is pushed to Firestore when possible.
- **Manual sync:** the Sync action uploads all products, customers, orders (with lines and payments), settings and the business profile. Uploads run in parallel groups, so large datasets sync quickly.
- **Restore:** when the local database has no orders and no customers (a fresh install or after logout), the app restores everything from Firestore on sign-in.
- **Duplicate cleanup:** products sharing the same name and business are merged during restore and sync. The lowest ID is kept, order lines are re-pointed to it, and the extra copies are removed locally and in Firestore.
- **Logout:**
  1. Everything is synced to the cloud first (with a timeout and a clear error if it fails).
  2. Local data, cached files, the saved logo and the App Lock setting are wiped.
  3. The user is signed out and returned to Google sign-in.
  - If the sync fails, the user can cancel or log out anyway, and is warned that unsynced changes will be lost.

---

## Security

- **Per-user isolation:** Firestore rules only allow a signed-in user to read and write their own `users/{uid}/...` data, and validate the shape of every document.
- **App Lock:** turn it on in Profile. The app asks for fingerprint, face or device PIN on launch and after being in the background for more than 60 seconds. Enabling or disabling it requires authentication. The setting is stored on the device only.
- **No secrets in the repo:** Firebase config and signing keys come from GitHub Actions secrets.
- **Profit masking:** hide profit figures on screen from Settings.

Test the rules with the Firestore emulator (see [Testing](#testing)).

---

## PDF documents and branding

Quotations and receipts are generated as A4 PDFs and previewed in the app before sharing.

- The header uses your **custom logo** when "Show logo on documents" is on and a logo has been uploaded. If no custom logo exists, it falls back to the aMACEzing app logo.
- Business details, footers, payment instructions and quotation terms come from the business profile (Profile → Business Branding).
- The footer reads "Generated by aMACEzing Sales Monitoring System".

---

## Importing and exporting data

- **Excel migration:** paste or load CSV content exported from your existing workbook, choose the business and sheet, preview the parsed rows and validation results, then import. Duplicate order numbers are detected.
- **CSV export:** export transactions from the Transactions screen or the Data Import screen and share the file through any app.

Open the Data Import screen from **Profile → Data Import**.

---

## CI/CD and releases

Two GitHub Actions workflows live in `.github/workflows/`. See `BUILD_AND_RELEASE.md` for details.

### Required repository secrets

| Secret | Used by | Purpose |
| --- | --- | --- |
| `GOOGLE_SERVICES_JSON` | both | Contents of `google-services.json` (raw JSON or base64) |
| `DEBUG_KEYSTORE_BASE64` | both | Base64 of a stable `debug.keystore`, so the debug SHA-1 never changes |
| `RELEASE_KEYSTORE_BASE64` | release | Base64 of the release keystore |
| `STORE_PASSWORD` | release | Keystore password |
| `KEY_PASSWORD` | release | Key password |

### Android Build & Test (debug)

Runs on pushes to `main`, `master` and `WF-**` branches, on pull requests, and manually. It builds the debug APK, runs unit tests and uploads the APK as an artifact.

### Android Release APK Build

Run it manually from **Actions → Android Release APK Build → Run workflow**.

- **Version number is automatic:** `YY.M.D.N`, for example `26.10.21.1`. `N` increases for each release on the same day and restarts at `1` on the next day. The date uses the Asia/Manila timezone.
- **Version code:** the workflow run number, so it always increases and Android accepts updates.
- **Release notes:** type your own in the input, or leave it empty to use the **last commit message**.
- **Output:** a signed APK named `aMACEzing-Sales-v<version>.apk`, uploaded as a workflow artifact and published as a **GitHub Release** tagged `v<version>-b<build>`.

### Signing

Keep the same keystore for every release. Android only installs an update over an existing app if both APKs are signed with the same key, and Firebase Google sign-in depends on the signing SHA-1.

- The key alias must be `amacezing`.
- To create a release keystore:
  ```bash
  keytool -genkeypair -v -keystore release.jks -alias amacezing \
    -keyalg RSA -keysize 2048 -validity 10000
  base64 -w0 release.jks   # paste into RELEASE_KEYSTORE_BASE64
  ```
- Back up the keystore and its passwords somewhere safe. If it is lost, updates can no longer be installed over existing installs.
- The workflow fails on purpose if no stable key is configured, rather than producing an APK with a random key.

---

## In-app updates

Release builds check the repository's latest GitHub Release on launch, and from **Settings/Profile → Check for updates**.

- The release tag format `vYY.M.D.N-b<build>` carries the build number the app compares against.
- If a newer build exists, the app shows the version and release notes, downloads the APK and opens the Android install prompt. Android requires the user to confirm every install.
- The first install of an updatable version must be done by hand, and every update after that can be in-app.
- The repository must be **public** for the updater to read releases without a token.
- Updates are only enabled in release builds, where the workflow passes the repository name to the build.

---

## Testing

Unit tests are in `app/src/test/`:

| Test | Covers |
| --- | --- |
| `SalesCalculatorTest` | Gross, net, discount and profit calculations |
| `FirestoreRepositoryParsingTest` | Firestore document parsing |
| `PdfAndQuotationTest` | Quotation and PDF logic |
| `FirestoreSalesRepositoryRuleTest` | Repository behaviour against security rules |
| `ExampleRobolectricTest` | Robolectric setup |

```bash
./gradlew :app:testDebugUnitTest
```

**Security rules tests** (`firestore.test.js`) run against the Firestore emulator:

```bash
npm install @firebase/rules-unit-testing
firebase emulators:exec --only firestore "node --test firestore.test.js"
```

The emulator host and port come from `firebase.json` (Firestore on `127.0.0.1:8085`).

---

## Troubleshooting

| Problem | Likely cause and fix |
| --- | --- |
| `PERMISSION_DENIED` on sync | Rules published in Firebase are older than `firestore.rules`, or the app targets a different database than the one the rules were published to. Check the database ID and republish. The sync error message names the failing step. |
| Google sign-in fails or "configuration missing" | `google-services.json` is missing, or the signing SHA-1 is not registered in Firebase. Add the SHA-1 printed by the "Show APK Signing Fingerprints" workflow step. |
| SHA-1 changes on every build | No stable keystore. Add `DEBUG_KEYSTORE_BASE64` (and the release secrets). |
| Update fails to install ("App not installed") | The new APK is signed with a different key than the installed one. Uninstall once and install a build signed with the stable keystore. |
| Logout says it couldn't back up | No internet or a Firestore error. The dialog shows the reason. Fix it and retry, or choose "Log out anyway" to discard unsynced changes. |
| Duplicate products after restore | Run Sync. Duplicates with the same name and business are merged automatically. |
| Release build fails on keystore | Set `RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD` and `KEY_PASSWORD`, or `DEBUG_KEYSTORE_BASE64`, and make sure the key alias is `amacezing`. |
| `libs.androidx.* unresolved` | `gradle/libs.versions.toml` is out of date relative to `app/build.gradle.kts`. Update both together. |

---

## Contributing

1. Create a branch from `main`.
2. Keep changes focused, and update tests when behaviour changes.
3. Make sure `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` pass.
4. If you change Firestore document shapes, update `firestore.rules` and `firestore.test.js` in the same pull request, and redeploy the rules.
5. Open a pull request. The debug build workflow runs automatically.

---

## License

No license file is included in this repository, so all rights are reserved by default. Contact the repository owner before reusing the code.
