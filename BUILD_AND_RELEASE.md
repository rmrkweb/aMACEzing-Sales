# aMACEzing Sales — Build & Release Guide

This document details how **aMACEzing Sales** is built, tested, and released automatically using **GitHub Actions**, as well as instructions for downloading and installing the APK directly on Android devices.

---

## 1. Project Overview & Architecture

**aMACEzing Sales** is a native Android application built with **Kotlin** and **Jetpack Compose (Material 3)**. It provides comprehensive sales tracking, invoicing, customer management, inventory cataloging, profit calculation, and cross-device synchronization for two integrated businesses:
- **Engraving Services**
- **Sintra Board Printing Services**

### Key Technologies
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Architecture:** MVVM with Kotlin Coroutines & StateFlow
- **Local Database:** Android Jetpack Room (with KSP code generation)
- **Cloud Database & Auth:** Firebase Cloud Firestore & Firebase Authentication
- **Build System:** Gradle 9.3.1 with Android Gradle Plugin (AGP) 9.1.1, Kotlin 2.2.10, and KSP 2.3.5

### Repository File Structure
```
.
├── .github/
│   └── workflows/
│       ├── android-build.yml       # Automated debug APK build & unit testing
│       └── android-release.yml     # Manual release APK build workflow
├── app/
│   ├── build.gradle.kts           # Application module build script & dependencies
│   ├── google-services.json       # Firebase project configuration
│   ├── proguard-rules.pro         # R8/ProGuard obfuscation & optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # App declaration & permissions
│       │   ├── java/com/example/   # Kotlin source code (UI, ViewModel, Data, Models)
│       │   └── res/                # App icons, colors, strings, adaptive drawables
│       └── test/                   # Unit & Robolectric test suites
├── gradle/
│   ├── libs.versions.toml         # Centralized Gradle version catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar     # Gradle wrapper binary
│       └── gradle-wrapper.properties # Gradle 9.3.1 configuration
├── build.gradle.kts               # Root Gradle build script
├── settings.gradle.kts            # Project repositories & module definitions
├── gradle.properties              # JVM options & AndroidX flags
├── gradlew / gradlew.bat          # Executable Gradle wrapper scripts
├── debug.keystore.base64          # Encoded debug signing key (for automated CI builds)
├── .env.example                   # Template for optional API secrets
└── BUILD_AND_RELEASE.md           # This documentation
```

---

## 2. Automated Builds via GitHub Actions

Two automated workflows are configured in `.github/workflows/`:

### A. Debug Build Workflow (`android-build.yml`)
- **Triggers:**
  - Every `push` to `main` or `master`
  - Every `pull_request` to `main` or `master`
  - Manual trigger via `workflow_dispatch`
- **Actions Performed:**
  1. Checks out the repository using `actions/checkout@v4`.
  2. Sets up JDK 21 (Temurin distribution).
  3. Caches Gradle dependencies using `gradle/actions/setup-gradle@v4`.
  4. Decodes the pre-configured `debug.keystore` from `debug.keystore.base64`.
  5. Compiles and packages the debug APK as primary step: `./gradlew :app:assembleDebug`.
  6. Verifies and immediately uploads the generated APK as a workflow artifact named `aMACEzing-Sales-debug-apk`.
  7. Executes unit tests if test files exist, reporting results without blocking APK artifact download.

### B. Release Build Workflow (`android-release.yml`)
- **Triggers:**
  - Manually initiated via the **Run workflow** button on GitHub Actions.
- **Actions Performed:**
  1. Checks out the repository and sets up JDK 21 + Gradle caching.
  2. Inspects repository secrets for production release signing keys (`RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`).
  3. Runs unit tests and builds `./gradlew :app:assembleRelease`.
  4. Uploads the production release APK as an artifact named `aMACEzing-Sales-release-apk`.

---

## 3. How to Trigger a Build on GitHub

1. Navigate to your repository on GitHub.
2. Click on the **Actions** tab located at the top.
3. In the left sidebar, select either:
   - **Android Build & Test (Debug APK)** for a test build.
   - **Android Release APK Build** for a release build.
4. Click the **Run workflow** dropdown on the right side.
5. Select the branch (e.g. `main`) and click the green **Run workflow** button.
6. The workflow will queue and run. A green checkmark indicates that the build and tests passed successfully.

---

## 4. How to Download the Built APK

1. Once the GitHub Actions workflow run finishes, click on the completed run title (e.g. "Android Build & Test (Debug APK)").
2. Scroll down to the bottom of the summary page to the **Artifacts** section.
3. Click on **aMACEzing-Sales-debug-apk** (or **aMACEzing-Sales-release-apk**).
4. GitHub will download a `.zip` archive containing the `.apk` file:
   - Debug APK: `app-debug.apk`
   - Release APK: `app-release.apk`
5. Extract the `.zip` archive on your computer or mobile device.

---

## 5. How to Install the Debug APK on an Android Device

1. **Transfer the APK to your phone:**
   - Download the APK directly using your phone's browser, or transfer it from your computer via USB, Google Drive, or messaging.
2. **Enable Unknown Apps Installation:**
   - When tapping the `.apk` file to install, Android may prompt: *"For your security, your phone is not allowed to install unknown apps from this source."*
   - Tap **Settings** and toggle **Allow from this source** for your file manager or browser.
3. **Complete the Installation:**
   - Tap **Install**.
   - If Google Play Protect displays a prompt *"Unrecognized app details"*, tap **More details** and then **Install anyway** (standard for debug-signed development builds).
4. Launch **aMACEzing Sales** from your app drawer.

---

## 6. Configuring Secure Production Release Signing

To sign release builds with your private keystore without committing sensitive passwords to Git:

### Step 1: Generate a Keystore (if you do not have one)
Run the following command on your local terminal:
```bash
keytool -genkey -v -keystore my-upload-key.jks \
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
Store this keystore file and passwords in a secure password manager.

### Step 2: Convert Keystore to Base64
Encode the keystore file to base64 so it can be stored as a GitHub secret:
- **macOS / Linux:**
  ```bash
  base64 -w 0 my-upload-key.jks > keystore_base64.txt
  # or on macOS:
  base64 -i my-upload-key.jks | tr -d '\n' > keystore_base64.txt
  ```
- **Windows PowerShell:**
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) | Out-File -Encoding ASCII keystore_base64.txt
  ```

### Step 3: Add GitHub Repository Secrets
1. In your GitHub repository, navigate to **Settings** > **Secrets and variables** > **Actions**.
2. Click **New repository secret** and add the following:
   - `RELEASE_KEYSTORE_BASE64`: Paste the entire content of `keystore_base64.txt`.
   - `STORE_PASSWORD`: The keystore password chosen during `keytool` creation.
   - `KEY_PASSWORD`: The private key password.

When you run **Android Release APK Build**, the workflow will automatically decode the keystore and sign the release APK with your official upload certificate.

---

## 7. Firebase Integration & Google Services

The project uses Firebase Cloud Firestore and Authentication.

### Configuration Status
- `app/google-services.json` is bundled in the application module with package name `com.amacezing.sales`.
- In `app/build.gradle.kts`, `missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN` is configured so that missing credentials provide clear warnings rather than unrecoverable build crashes.

### Linking Your Own Firebase Project
If you want to connect a custom Firebase project:
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a project and add an Android app with package name `com.amacezing.sales`.
3. Add your SHA-1 fingerprint (from your debug or release keystore).
   - For `debug.keystore`, run:
     ```bash
     keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android
     ```
4. Download the new `google-services.json` and replace `app/google-services.json`.
5. Enable **Authentication** (Email/Password and Google Sign-In) and **Cloud Firestore** in test or production mode.

---

## 8. Differences: Debug vs. Release APK

| Feature | Debug APK (`app-debug.apk`) | Release APK (`app-release.apk`) |
| :--- | :--- | :--- |
| **Purpose** | Testing, internal review, rapid deployment | Production distribution, Google Play, end-users |
| **Signing** | Pre-configured `debug.keystore` (automated) | Custom private certificate or upload key |
| **Code Shrinking & R8** | Disabled for fast compilation | Enabled (via `proguard-rules.pro`) |
| **Debugging** | `android:debuggable="true"` for logging | Optimized bytecode; debugging disabled |
| **Installation** | Sideload directly to any Android device | Sideload or publish to app stores |

---

## 9. Troubleshooting & Common Questions

### `Execution failed for task ':app:testDebugUnitTest'`
- Pure logic tests (`SalesCalculatorTest`, `ExampleRobolectricTest`) run in all CI environments without dependencies.
- Cloud Firestore rule emulator tests (`FirestoreSalesRepositoryRuleTest`) gracefully check for a running Firebase emulator on `127.0.0.1:8085` and automatically skip if the emulator is not active, preventing false CI failures.

### `Missing debug.keystore in GitHub Actions`
- `debug.keystore.base64` is tracked in the repository. The GitHub Actions workflow decodes it automatically on line:
  ```bash
  base64 -d debug.keystore.base64 > debug.keystore
  ```
- If neither exists, the workflow includes a fallback `keytool` generation step so builds never halt.

### `Unsupported class file major version`
- Ensure the runner or your local workstation uses **Java 17 or Java 21**. Gradle 9.3.1 requires Java 17+. The GitHub Actions workflow explicitly specifies `java-version: '21'` with `distribution: 'temurin'`.

## Cloud-to-local restore fix (October 2026)

The app now attempts to restore existing Firestore products, customers, transactions, and settings into Room after sign-in when this installation has no local transactions/customers. Manual Sync also attempts that restore before uploading local records, preventing an empty fresh installation from immediately uploading empty lists over the cloud backup. Restoration merges records and does not clear local tables.

Important limitation: the current Firestore transaction model stores only `lineItemsSummary` (a text summary), not structured transaction line items or individual payment records. Restored transactions retain their sales totals and aggregate payment fields, but line-item breakdowns and payment history cannot be reconstructed from the existing cloud schema. To restore those exactly, the app must also sync structured line-item and payment documents in a separate schema migration.

Build validation could not be completed in the editing environment because Gradle distribution download requires network access. Run the project's GitHub Actions Android build before installing the new APK.
