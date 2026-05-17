# Tides Plan 1: Foundation & Crypto Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the project scaffold and security-critical foundation: Gradle/Hilt setup, the `crypto/` module (Argon2id + Keystore), the auth-metadata file format, SQLCipher-backed Room database, and a verified lock/unlock primitive. End state: an empty app that can set a PIN, encrypt its database, and lock/unlock with PIN or biometric — all proven by tests.

**Architecture:** Single Gradle module, package-level boundaries. The `crypto/` package has zero outward dependencies and is the only place key material is touched. PIN → Argon2id (64 MiB, 3 iter) → 32-byte key → SQLCipher database. Biometric unlock is implemented as a Keystore-wrapped copy of the derived key, unwrappable only with `BiometricPrompt` confirmation. The `auth_meta.bin` file stores Argon2 salts, PIN hashes (separate Argon2 derivation for validation, distinct salt from the key derivation), fail count, and cooldown expiry.

**Tech Stack:**
- Kotlin 2.0.x, Java 17 toolchain
- Gradle 8.x with Kotlin DSL
- AGP (Android Gradle Plugin) 8.5+
- Compose BoM 2024.10+, Material 3
- Hilt 2.52
- Room 2.6+ with SQLCipher for Android (`net.zetetic:sqlcipher-android:4.5.4`)
- Argon2 via `com.lambdapioneer.argon2kt:argon2kt` (JVM-friendly, ships native Argon2 for ARM/x86)
- Biometric: `androidx.biometric:biometric:1.2.0-alpha05`
- Keystore: Android Keystore via `javax.crypto.Cipher` with `AndroidKeyStore` provider
- Testing: JUnit5 (via `org.junit.jupiter:junit-jupiter:5.10+`), Robolectric, Compose UI Test, MockK

**Out of scope for this plan:**
- Onboarding UI flow (Plan 3)
- Settings, threat-model preset picker UI (Plan 3)
- Calendar, log sheet, stats (Plans 2 & 3)
- Notifications, widget, PDF/CSV export (Plan 4)
- F-Droid metadata, CI signing pipeline (Plan 4)

What this plan *does* produce: a runnable app that boots to a single placeholder screen, with a working underlying lock primitive (PIN setup, derive key, encrypt DB, lock, unlock) and a passing security-critical test suite covering all §4 invariants.

---

## File structure (created by this plan)

**Build files:**
- `build.gradle.kts` (root)
- `settings.gradle.kts`
- `gradle/libs.versions.toml` (version catalog)
- `app/build.gradle.kts`
- `app/proguard-rules.pro`

**Manifest:**
- `app/src/main/AndroidManifest.xml`

**Source — application:**
- `app/src/main/java/com/hayate0726/tides/TidesApplication.kt`
- `app/src/main/java/com/hayate0726/tides/MainActivity.kt`

**Source — crypto package (zero outward deps):**
- `app/src/main/java/com/hayate0726/tides/crypto/Argon2.kt` — Argon2id wrapper
- `app/src/main/java/com/hayate0726/tides/crypto/KeyDerivation.kt` — PIN → DbKey
- `app/src/main/java/com/hayate0726/tides/crypto/KeystoreWrapper.kt` — Keystore wrap/unwrap of derived key
- `app/src/main/java/com/hayate0726/tides/crypto/DbKey.kt` — value class wrapping 32-byte key with zero() method
- `app/src/main/java/com/hayate0726/tides/crypto/AuthMeta.kt` — `auth_meta.bin` read/write
- `app/src/main/java/com/hayate0726/tides/crypto/Pin.kt` — value class wrapping a PIN with zero() method

**Source — data package:**
- `app/src/main/java/com/hayate0726/tides/data/TidesDatabase.kt` — Room database (initially empty schema with a single placeholder entity for the SQLCipher roundtrip test)
- `app/src/main/java/com/hayate0726/tides/data/DatabaseFactory.kt` — opens/closes SQLCipher DB with a `DbKey`

**Source — lock manager:**
- `app/src/main/java/com/hayate0726/tides/lock/LockManager.kt` — top-level lock state holder; gates UI

**Source — Hilt wiring:**
- `app/src/main/java/com/hayate0726/tides/di/CryptoModule.kt`
- `app/src/main/java/com/hayate0726/tides/di/DataModule.kt`

**Tests:**
- `app/src/test/java/com/hayate0726/tides/crypto/Argon2Test.kt`
- `app/src/test/java/com/hayate0726/tides/crypto/KeyDerivationTest.kt`
- `app/src/test/java/com/hayate0726/tides/crypto/AuthMetaTest.kt`
- `app/src/test/java/com/hayate0726/tides/crypto/DbKeyZeroingTest.kt`
- `app/src/androidTest/java/com/hayate0726/tides/crypto/KeystoreWrapperTest.kt` (instrumented; needs Keystore)
- `app/src/androidTest/java/com/hayate0726/tides/data/DatabaseFactoryTest.kt` (instrumented; SQLCipher integration)
- `app/src/androidTest/java/com/hayate0726/tides/security/NoInternetPermissionTest.kt` (instrumented; APK manifest audit)
- `app/src/androidTest/java/com/hayate0726/tides/security/NoSensitiveLogsTest.kt` (instrumented; Logcat grep)
- `app/src/androidTest/java/com/hayate0726/tides/security/KeyNotOnDiskTest.kt` (instrumented; grep app-private files for key bytes)
- `app/src/androidTest/java/com/hayate0726/tides/security/WrongPinBruteForceTest.kt` (instrumented; 1000 random wrong PINs)
- `app/src/androidTest/java/com/hayate0726/tides/security/RateLimitTest.kt` (instrumented; 5 wrong attempts → cooldown)

**CI:**
- `.github/workflows/ci.yml` — build + test + manifest audit on every PR

---

## Task 1: Initialize Gradle project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `.gitignore`

- [ ] **Step 1: Create `.gitignore` so we don't commit build artifacts**

Create `.gitignore`:

```gitignore
# Gradle
.gradle/
build/
out/

# IntelliJ / Android Studio
.idea/
*.iml
local.properties
captures/
.externalNativeBuild/
.cxx/

# Kotlin
*.kotlin_module

# OS
.DS_Store
Thumbs.db

# Keystore
*.jks
*.keystore
keystore.properties

# Coverage
*.exec
coverage/
```

- [ ] **Step 2: Create `gradle.properties`**

Create `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

android.useAndroidX=true
android.nonTransitiveRClass=true

kotlin.code.style=official
```

- [ ] **Step 3: Create version catalog `gradle/libs.versions.toml`**

Create `gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
hilt = "2.52"
compose-bom = "2024.10.00"
compose-compiler = "1.5.15"
room = "2.6.1"
sqlcipher = "4.5.4"
argon2kt = "1.6.0"
biometric = "1.2.0-alpha05"
coroutines = "1.9.0"
junit5 = "5.11.3"
mockk = "1.13.13"
robolectric = "4.13"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.13.1" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version = "2.8.7" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.3" }
androidx-biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }

compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }

hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

sqlcipher = { group = "net.zetetic", name = "sqlcipher-android", version.ref = "sqlcipher" }
argon2kt = { group = "com.lambdapioneer.argon2kt", name = "argon2kt", version.ref = "argon2kt" }

kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Test
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit5" }
junit-jupiter-params = { group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
mockk-android = { group = "io.mockk", name = "mockk-android", version.ref = "mockk" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version = "1.2.1" }
androidx-test-runner = { group = "androidx.test", name = "runner", version = "1.6.2" }
androidx-test-rules = { group = "androidx.test", name = "rules", version = "1.6.1" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 4: Create `settings.gradle.kts`**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tides"
include(":app")
```

- [ ] **Step 5: Create root `build.gradle.kts`**

Create `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 6: Commit**

```bash
cd /home/hayate0726/cycles
git add settings.gradle.kts build.gradle.kts gradle/libs.versions.toml gradle.properties .gitignore
git commit -m "chore: initialize Gradle project structure"
```

---

## Task 2: Create the :app module

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/hayate0726/tides/TidesApplication.kt`
- Create: `app/src/main/java/com/hayate0726/tides/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Create `app/build.gradle.kts`**

Create `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.hayate0726.tides"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hayate0726.tides"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.sqlcipher)
    implementation(libs.argon2kt)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Unit tests (JVM)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.ext.junit)

    // Instrumented tests
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.mockk.android)
}
```

- [ ] **Step 2: Create `app/proguard-rules.pro`**

Create `app/proguard-rules.pro`:

```
# Keep SQLCipher
-keep class net.zetetic.** { *; }
-keep class com.lambdapioneer.argon2kt.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Keep Room
-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
```

- [ ] **Step 3: Create `AndroidManifest.xml` (NOTE: no INTERNET permission)**

Create `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- DELIBERATELY NO INTERNET PERMISSION. The app cannot make a network call. -->
    <!-- See docs/superpowers/specs/2026-05-15-tides-design.md §4 invariant 5. -->

    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".TidesApplication"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="false"
        android:icon="@android:drawable/ic_menu_my_calendar"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Tides"
        tools:targetApi="33">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.Tides">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: Create backup rules to disable Android system backup**

Create `app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" />
        <exclude domain="file" />
        <exclude domain="database" />
        <exclude domain="sharedpref" />
        <exclude domain="external" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" />
        <exclude domain="file" />
        <exclude domain="database" />
        <exclude domain="sharedpref" />
        <exclude domain="external" />
    </device-transfer>
</data-extraction-rules>
```

Also create legacy backup rules `app/src/main/res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="root" />
    <exclude domain="file" />
    <exclude domain="database" />
    <exclude domain="sharedpref" />
    <exclude domain="external" />
</full-backup-content>
```

- [ ] **Step 5: Create `strings.xml` and `themes.xml`**

Create `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Tides</string>
</resources>
```

Create `app/src/main/res/values/themes.xml`:

```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.Tides" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowLightStatusBar" tools:targetApi="m">true</item>
    </style>
</resources>
```

- [ ] **Step 6: Create `TidesApplication.kt`**

Create `app/src/main/java/com/hayate0726/tides/TidesApplication.kt`:

```kotlin
package com.hayate0726.tides

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TidesApplication : Application()
```

- [ ] **Step 7: Create `MainActivity.kt`**

Create `app/src/main/java/com/hayate0726/tides/MainActivity.kt`:

```kotlin
package com.hayate0726.tides

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Placeholder()
                }
            }
        }
    }
}

@Composable
private fun Placeholder() {
    Text(
        text = "Tides — foundation layer",
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    )
}
```

- [ ] **Step 8: Build the project to verify scaffold is correct**

Run:

```bash
cd /home/hayate0726/cycles
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. If gradle wrapper doesn't exist yet, run `gradle wrapper --gradle-version 8.9` first to create it (the Gradle binary needs to be installed; this plan assumes a host Gradle).

- [ ] **Step 9: Commit**

```bash
git add app/build.gradle.kts app/proguard-rules.pro app/src
git commit -m "feat(app): scaffold :app module with empty placeholder UI"
```

---

## Task 3: First security-critical test — no INTERNET permission

**Files:**
- Create: `app/src/androidTest/java/com/hayate0726/tides/security/NoInternetPermissionTest.kt`

We do this test FIRST, before any other code, because it's a release-blocker invariant from spec §4 #5. The test must pass continuously.

- [ ] **Step 1: Write the failing test**

Create `app/src/androidTest/java/com/hayate0726/tides/security/NoInternetPermissionTest.kt`:

```kotlin
package com.hayate0726.tides.security

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoInternetPermissionTest {

    @Test
    fun apk_must_not_declare_internet_permission() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val pkg = ctx.packageManager.getPackageInfo(
            ctx.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val perms = pkg.requestedPermissions.orEmpty().toList()
        assertFalse(
            "INTERNET permission must not appear in manifest. Found: $perms",
            perms.contains(android.Manifest.permission.INTERNET)
        )
    }

    @Test
    fun apk_must_not_declare_access_network_state_permission() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val pkg = ctx.packageManager.getPackageInfo(
            ctx.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val perms = pkg.requestedPermissions.orEmpty().toList()
        assertFalse(
            "ACCESS_NETWORK_STATE must not appear. Found: $perms",
            perms.contains(android.Manifest.permission.ACCESS_NETWORK_STATE)
        )
    }
}
```

- [ ] **Step 2: Run on emulator/device to verify it passes**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.security.NoInternetPermissionTest"
```

Expected: 2 tests pass. (The manifest from Task 2 already excludes INTERNET, so the test passes immediately. This is intentional — we're locking in the invariant from day one.)

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/hayate0726/tides/security/NoInternetPermissionTest.kt
git commit -m "test(security): assert APK never declares INTERNET permission"
```

---

## Task 4: Value classes for sensitive byte arrays (Pin, DbKey)

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/crypto/Pin.kt`
- Create: `app/src/main/java/com/hayate0726/tides/crypto/DbKey.kt`
- Create: `app/src/test/java/com/hayate0726/tides/crypto/DbKeyZeroingTest.kt`

Goal: every sensitive byte array (PIN, derived key) is wrapped in a small class that exposes `zero()` to wipe the underlying bytes. Bypasses the JVM's normal "let GC handle it" laziness for crypto material.

- [ ] **Step 1: Write the failing zeroing test**

Create `app/src/test/java/com/hayate0726/tides/crypto/DbKeyZeroingTest.kt`:

```kotlin
package com.hayate0726.tides.crypto

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows

class DbKeyZeroingTest {

    @Test
    fun `zero wipes the underlying bytes`() {
        val original = ByteArray(32) { (it + 1).toByte() }
        val key = DbKey(original)
        key.zero()
        // After zero, the bytes the caller still holds a reference to are also wiped
        assertArrayEquals(ByteArray(32), original)
    }

    @Test
    fun `accessing bytes after zero throws`() {
        val key = DbKey(ByteArray(32) { 1 })
        key.zero()
        assertThrows(IllegalStateException::class.java) {
            key.bytes
        }
    }

    @Test
    fun `pin zero wipes underlying char array`() {
        val original = "123456".toCharArray()
        val pin = Pin(original)
        pin.zero()
        assertArrayEquals(CharArray(6) { 0.toChar() }, original)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.crypto.DbKeyZeroingTest"
```

Expected: FAIL with "unresolved reference: DbKey" / "unresolved reference: Pin"

- [ ] **Step 3: Implement `DbKey`**

Create `app/src/main/java/com/hayate0726/tides/crypto/DbKey.kt`:

```kotlin
package com.hayate0726.tides.crypto

import java.util.Arrays

/**
 * Wraps a 32-byte database encryption key. The underlying byte array is
 * the caller's; `zero()` wipes it in place. After `zero()`, `bytes` throws.
 *
 * Required size: 32 bytes (256 bits).
 */
class DbKey(bytes: ByteArray) {
    init {
        require(bytes.size == REQUIRED_SIZE) {
            "DbKey must be exactly $REQUIRED_SIZE bytes, got ${bytes.size}"
        }
    }

    private var _bytes: ByteArray? = bytes

    val bytes: ByteArray
        get() = _bytes ?: throw IllegalStateException("DbKey has been zeroed")

    fun zero() {
        _bytes?.let { Arrays.fill(it, 0) }
        _bytes = null
    }

    val isZeroed: Boolean get() = _bytes == null

    companion object {
        const val REQUIRED_SIZE = 32
    }
}
```

- [ ] **Step 4: Implement `Pin`**

Create `app/src/main/java/com/hayate0726/tides/crypto/Pin.kt`:

```kotlin
package com.hayate0726.tides.crypto

import java.util.Arrays

/**
 * Wraps a PIN or passphrase as a CharArray (chars, not String, because
 * String is immutable and lingers in the JVM string pool).
 *
 * `zero()` overwrites the underlying chars and detaches the reference.
 */
class Pin(chars: CharArray) {
    init {
        require(chars.isNotEmpty()) { "PIN must not be empty" }
    }

    private var _chars: CharArray? = chars

    val chars: CharArray
        get() = _chars ?: throw IllegalStateException("Pin has been zeroed")

    /**
     * UTF-8 bytes of the PIN. Caller is responsible for zeroing the
     * returned array if it holds it.
     */
    fun toUtf8Bytes(): ByteArray {
        val chars = _chars ?: throw IllegalStateException("Pin has been zeroed")
        return String(chars).toByteArray(Charsets.UTF_8)
    }

    fun zero() {
        _chars?.let { Arrays.fill(it, 0.toChar()) }
        _chars = null
    }

    val isZeroed: Boolean get() = _chars == null
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.crypto.DbKeyZeroingTest"
```

Expected: 3 tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/crypto/DbKey.kt \
        app/src/main/java/com/hayate0726/tides/crypto/Pin.kt \
        app/src/test/java/com/hayate0726/tides/crypto/DbKeyZeroingTest.kt
git commit -m "feat(crypto): add Pin and DbKey value classes with zero() semantics"
```

---

## Task 5: Argon2id wrapper

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/crypto/Argon2.kt`
- Create: `app/src/test/java/com/hayate0726/tides/crypto/Argon2Test.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/crypto/Argon2Test.kt`:

```kotlin
package com.hayate0726.tides.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

/**
 * Note: Argon2kt ships native code. These tests run on the JVM via Robolectric
 * if the platform supports it; on hosts without the right native arch they will
 * be skipped. Full coverage relies on instrumented tests on real Android.
 */
@EnabledOnOs(OS.LINUX, OS.MAC, OS.WINDOWS)
class Argon2Test {

    @Test
    fun `derive produces 32-byte output`() {
        val out = Argon2.derive(
            password = "test-pin".toByteArray(),
            salt = ByteArray(16) { 7 },
            params = Argon2.Params.DEFAULT
        )
        assertEquals(32, out.size)
    }

    @Test
    fun `derive is deterministic for same inputs`() {
        val a = Argon2.derive("pin".toByteArray(), ByteArray(16), Argon2.Params.DEFAULT)
        val b = Argon2.derive("pin".toByteArray(), ByteArray(16), Argon2.Params.DEFAULT)
        assertArrayEquals(a, b)
    }

    @Test
    fun `different salts produce different outputs`() {
        val a = Argon2.derive("pin".toByteArray(), ByteArray(16) { 1 }, Argon2.Params.DEFAULT)
        val b = Argon2.derive("pin".toByteArray(), ByteArray(16) { 2 }, Argon2.Params.DEFAULT)
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun `salt shorter than 16 bytes throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Argon2.derive("pin".toByteArray(), ByteArray(8), Argon2.Params.DEFAULT)
        }
    }

    @Test
    fun `default params match spec`() {
        val p = Argon2.Params.DEFAULT
        assertEquals(64 * 1024, p.memoryCostKib)
        assertEquals(3, p.iterations)
        assertEquals(1, p.parallelism)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.crypto.Argon2Test"
```

Expected: FAIL with "unresolved reference: Argon2"

- [ ] **Step 3: Implement `Argon2`**

Create `app/src/main/java/com/hayate0726/tides/crypto/Argon2.kt`:

```kotlin
package com.hayate0726.tides.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode

/**
 * Thin wrapper around argon2kt. All key/hash derivation in the app must use
 * this; never call argon2kt directly. Centralizing makes audit easier.
 *
 * Output is always 32 bytes (256 bits), enough for AES-256 / SQLCipher.
 */
object Argon2 {

    data class Params(
        val memoryCostKib: Int,
        val iterations: Int,
        val parallelism: Int,
    ) {
        companion object {
            /**
             * Tuned for ~300–500ms unlock latency on a midrange Android phone.
             * Memory cost = 64 MiB (well above OWASP 2024 floor of 19 MiB).
             * Iterations = 3, Parallelism = 1.
             * See spec §4.
             */
            val DEFAULT = Params(
                memoryCostKib = 64 * 1024,
                iterations = 3,
                parallelism = 1,
            )
        }
    }

    private const val SALT_MIN_BYTES = 16
    private const val HASH_BYTES = 32

    private val argon2 = Argon2Kt()

    /**
     * Derive a 32-byte key from `password` and `salt` using Argon2id.
     * `salt` must be at least 16 bytes. Caller is responsible for zeroing
     * `password` after this call returns.
     */
    fun derive(password: ByteArray, salt: ByteArray, params: Params): ByteArray {
        require(salt.size >= SALT_MIN_BYTES) {
            "salt must be at least $SALT_MIN_BYTES bytes, got ${salt.size}"
        }
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password,
            salt = salt,
            tCostInIterations = params.iterations,
            mCostInKibibyte = params.memoryCostKib,
            parallelism = params.parallelism,
            hashLengthInBytes = HASH_BYTES,
        )
        return result.rawHashAsByteArray()
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.crypto.Argon2Test"
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/crypto/Argon2.kt \
        app/src/test/java/com/hayate0726/tides/crypto/Argon2Test.kt
git commit -m "feat(crypto): add Argon2id wrapper with spec-default params (64MiB, 3 iter)"
```

---

## Task 6: KeyDerivation — PIN → DbKey

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/crypto/KeyDerivation.kt`
- Create: `app/src/test/java/com/hayate0726/tides/crypto/KeyDerivationTest.kt`

The KeyDerivation class is the **only** public API for turning a PIN into a key.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/crypto/KeyDerivationTest.kt`:

```kotlin
package com.hayate0726.tides.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KeyDerivationTest {

    @Test
    fun `deriveKey returns a 32-byte DbKey`() {
        val pin = Pin("123456".toCharArray())
        val salt = ByteArray(16) { 1 }
        val key = KeyDerivation.deriveKey(pin, salt)
        assertEquals(32, key.bytes.size)
        assertFalse(key.isZeroed)
    }

    @Test
    fun `same pin and salt produce same DbKey bytes`() {
        val pin1 = Pin("123456".toCharArray())
        val pin2 = Pin("123456".toCharArray())
        val salt = ByteArray(16) { 1 }
        val a = KeyDerivation.deriveKey(pin1, salt)
        val b = KeyDerivation.deriveKey(pin2, salt)
        assertArrayEquals(a.bytes, b.bytes)
    }

    @Test
    fun `different pins produce different keys`() {
        val a = KeyDerivation.deriveKey(Pin("111111".toCharArray()), ByteArray(16))
        val b = KeyDerivation.deriveKey(Pin("222222".toCharArray()), ByteArray(16))
        assertNotEquals(a.bytes.toList(), b.bytes.toList())
    }

    @Test
    fun `derivePinHash produces stable 32-byte hash`() {
        val h1 = KeyDerivation.derivePinHash(
            Pin("123456".toCharArray()),
            ByteArray(16) { 2 }
        )
        val h2 = KeyDerivation.derivePinHash(
            Pin("123456".toCharArray()),
            ByteArray(16) { 2 }
        )
        assertEquals(32, h1.size)
        assertArrayEquals(h1, h2)
    }

    @Test
    fun `pin hash and key are different (different salts)`() {
        val pin = Pin("123456".toCharArray())
        val key = KeyDerivation.deriveKey(pin, ByteArray(16) { 1 })
        val hash = KeyDerivation.derivePinHash(Pin("123456".toCharArray()), ByteArray(16) { 2 })
        assertNotEquals(key.bytes.toList(), hash.toList())
    }

    @Test
    fun `validatePin returns true for matching pin`() {
        val storedHash = KeyDerivation.derivePinHash(
            Pin("123456".toCharArray()),
            ByteArray(16) { 3 }
        )
        assertTrue(
            KeyDerivation.validatePin(
                Pin("123456".toCharArray()),
                ByteArray(16) { 3 },
                storedHash
            )
        )
    }

    @Test
    fun `validatePin returns false for wrong pin`() {
        val storedHash = KeyDerivation.derivePinHash(
            Pin("123456".toCharArray()),
            ByteArray(16) { 3 }
        )
        assertFalse(
            KeyDerivation.validatePin(
                Pin("999999".toCharArray()),
                ByteArray(16) { 3 },
                storedHash
            )
        )
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.crypto.KeyDerivationTest"
```

Expected: FAIL with "unresolved reference: KeyDerivation"

- [ ] **Step 3: Implement `KeyDerivation`**

Create `app/src/main/java/com/hayate0726/tides/crypto/KeyDerivation.kt`:

```kotlin
package com.hayate0726.tides.crypto

import java.security.MessageDigest

/**
 * The only public API for turning a PIN into key material.
 *
 * Two distinct derivations, each with its own salt:
 *
 *  - **deriveKey**: PIN + key-salt → 32-byte database key (used by SQLCipher).
 *  - **derivePinHash**: PIN + hash-salt → 32-byte hash (stored to validate the
 *    PIN before attempting DB open; lets us detect wrong PINs in O(Argon2)
 *    rather than O(SQLCipher-open), and lets us check for the duress PIN
 *    without ever opening the wrong database).
 *
 * Both derivations use the same Argon2 params (spec §4). The two salts must
 * be different — using the same salt for both would let an attacker who steals
 * the disk image equate "I have the hash" with "I have the key."
 *
 * Caller is responsible for managing the lifetime of the returned bytes.
 * `derive*` accept a `Pin` but do not zero it — the caller decides when.
 */
object KeyDerivation {

    fun deriveKey(pin: Pin, keySalt: ByteArray): DbKey {
        val pwd = pin.toUtf8Bytes()
        try {
            val raw = Argon2.derive(pwd, keySalt, Argon2.Params.DEFAULT)
            return DbKey(raw)
        } finally {
            java.util.Arrays.fill(pwd, 0)
        }
    }

    fun derivePinHash(pin: Pin, hashSalt: ByteArray): ByteArray {
        val pwd = pin.toUtf8Bytes()
        try {
            return Argon2.derive(pwd, hashSalt, Argon2.Params.DEFAULT)
        } finally {
            java.util.Arrays.fill(pwd, 0)
        }
    }

    /**
     * Constant-time comparison against the stored hash.
     * Returns true only if the PIN matches.
     */
    fun validatePin(pin: Pin, hashSalt: ByteArray, storedHash: ByteArray): Boolean {
        val candidate = derivePinHash(pin, hashSalt)
        try {
            return MessageDigest.isEqual(candidate, storedHash)
        } finally {
            java.util.Arrays.fill(candidate, 0)
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.crypto.KeyDerivationTest"
```

Expected: 7 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/crypto/KeyDerivation.kt \
        app/src/test/java/com/hayate0726/tides/crypto/KeyDerivationTest.kt
git commit -m "feat(crypto): add KeyDerivation (deriveKey, derivePinHash, validatePin)"
```

---

## Task 7: `auth_meta.bin` format

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/crypto/AuthMeta.kt`
- Create: `app/src/test/java/com/hayate0726/tides/crypto/AuthMetaTest.kt`

`auth_meta.bin` is a single file that stores:
- Magic bytes + version
- Primary key salt (16B)
- Primary PIN hash salt (16B)
- Primary PIN hash (32B)
- Optional duress key salt (16B), duress PIN hash salt (16B), duress PIN hash (32B), duress mode (1B: 0=off, 1=decoy, 2=wipe)
- Fail count (4B BE)
- Cooldown expiry epoch-millis (8B BE)

Total fixed size: 4 (magic) + 1 (version) + 1 (has_duress) + 1 (duress_mode) + 16 + 16 + 32 + 16 + 16 + 32 + 4 + 8 = 147 bytes. We always write all slots; the `has_duress` flag tells us whether the duress fields are populated.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/crypto/AuthMetaTest.kt`:

```kotlin
package com.hayate0726.tides.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class AuthMetaTest {

    @Test
    fun `round-trip without duress`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        val original = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = ByteArray(16) { 2 },
            pinHash = ByteArray(32) { 3 },
            duress = null,
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )
        AuthMeta.write(file, original)
        val read = AuthMeta.read(file)
        assertArrayEquals(original.keySalt, read.keySalt)
        assertArrayEquals(original.pinHashSalt, read.pinHashSalt)
        assertArrayEquals(original.pinHash, read.pinHash)
        assertNull(read.duress)
        assertEquals(0, read.failCount)
        assertEquals(0L, read.cooldownExpiryEpochMs)
    }

    @Test
    fun `round-trip with duress decoy`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        val original = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = ByteArray(16) { 2 },
            pinHash = ByteArray(32) { 3 },
            duress = AuthMeta.Duress(
                keySalt = ByteArray(16) { 4 },
                pinHashSalt = ByteArray(16) { 5 },
                pinHash = ByteArray(32) { 6 },
                mode = AuthMeta.DuressMode.DECOY,
            ),
            failCount = 2,
            cooldownExpiryEpochMs = 1_700_000_000_000L,
        )
        AuthMeta.write(file, original)
        val read = AuthMeta.read(file)
        assertNotNull(read.duress)
        assertEquals(AuthMeta.DuressMode.DECOY, read.duress!!.mode)
        assertArrayEquals(original.duress!!.pinHash, read.duress!!.pinHash)
        assertEquals(2, read.failCount)
        assertEquals(1_700_000_000_000L, read.cooldownExpiryEpochMs)
    }

    @Test
    fun `round-trip with duress wipe`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        val original = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = ByteArray(16) { 2 },
            pinHash = ByteArray(32) { 3 },
            duress = AuthMeta.Duress(
                keySalt = ByteArray(16) { 4 },
                pinHashSalt = ByteArray(16) { 5 },
                pinHash = ByteArray(32) { 6 },
                mode = AuthMeta.DuressMode.WIPE,
            ),
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )
        AuthMeta.write(file, original)
        val read = AuthMeta.read(file)
        assertEquals(AuthMeta.DuressMode.WIPE, read.duress!!.mode)
    }

    @Test
    fun `corrupted magic throws`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        file.writeBytes(ByteArray(147) { 0xFF.toByte() })
        try {
            AuthMeta.read(file)
            assertFalse(true, "expected throw")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("magic"))
        }
    }

    @Test
    fun `wrong length throws`(@TempDir tmp: Path) {
        val file = File(tmp.toFile(), "auth_meta.bin")
        file.writeBytes(ByteArray(50))
        try {
            AuthMeta.read(file)
            assertFalse(true, "expected throw")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("length"))
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.crypto.AuthMetaTest"
```

Expected: FAIL with "unresolved reference: AuthMeta"

- [ ] **Step 3: Implement `AuthMeta`**

Create `app/src/main/java/com/hayate0726/tides/crypto/AuthMeta.kt`:

```kotlin
package com.hayate0726.tides.crypto

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Fixed-size binary container for non-secret authentication metadata.
 *
 * Layout (147 bytes total, big-endian):
 *
 *  offset  size  field
 *  ------  ----  -------------------------------
 *       0     4  magic = 0x54_49_44_45  ("TIDE")
 *       4     1  version = 0x01
 *       5     1  has_duress (0 or 1)
 *       6     1  duress_mode (0=off, 1=decoy, 2=wipe)
 *       7    16  primary key_salt
 *      23    16  primary pin_hash_salt
 *      39    32  primary pin_hash
 *      71    16  duress key_salt (zeros if has_duress=0)
 *      87    16  duress pin_hash_salt (zeros if has_duress=0)
 *     103    32  duress pin_hash (zeros if has_duress=0)
 *     135     4  fail_count (uint32, big-endian)
 *     139     8  cooldown_expiry_epoch_ms (int64, big-endian)
 *
 * The file is allowed to exist on disk in plaintext: it contains
 * no key material, only salts (random by construction) and Argon2 hashes
 * of the PINs (computationally infeasible to invert).
 */
data class AuthMeta(
    val keySalt: ByteArray,
    val pinHashSalt: ByteArray,
    val pinHash: ByteArray,
    val duress: Duress?,
    val failCount: Int,
    val cooldownExpiryEpochMs: Long,
) {
    init {
        require(keySalt.size == 16) { "keySalt must be 16 bytes" }
        require(pinHashSalt.size == 16) { "pinHashSalt must be 16 bytes" }
        require(pinHash.size == 32) { "pinHash must be 32 bytes" }
    }

    data class Duress(
        val keySalt: ByteArray,
        val pinHashSalt: ByteArray,
        val pinHash: ByteArray,
        val mode: DuressMode,
    ) {
        init {
            require(keySalt.size == 16) { "duress keySalt must be 16 bytes" }
            require(pinHashSalt.size == 16) { "duress pinHashSalt must be 16 bytes" }
            require(pinHash.size == 32) { "duress pinHash must be 32 bytes" }
            require(mode != DuressMode.OFF) { "duress != null implies mode != OFF" }
        }
    }

    enum class DuressMode(val byte: Byte) {
        OFF(0), DECOY(1), WIPE(2);
        companion object {
            fun fromByte(b: Byte): DuressMode =
                values().firstOrNull { it.byte == b }
                    ?: throw IllegalStateException("Unknown duress mode byte: $b")
        }
    }

    companion object {
        private const val MAGIC: Int = 0x54_49_44_45  // "TIDE"
        private const val VERSION: Byte = 0x01
        const val FILE_SIZE: Int = 147

        fun write(file: File, meta: AuthMeta) {
            val buf = ByteBuffer.allocate(FILE_SIZE).order(ByteOrder.BIG_ENDIAN)
            buf.putInt(MAGIC)
            buf.put(VERSION)
            buf.put(if (meta.duress != null) 1.toByte() else 0.toByte())
            buf.put(meta.duress?.mode?.byte ?: DuressMode.OFF.byte)
            buf.put(meta.keySalt)
            buf.put(meta.pinHashSalt)
            buf.put(meta.pinHash)
            if (meta.duress != null) {
                buf.put(meta.duress.keySalt)
                buf.put(meta.duress.pinHashSalt)
                buf.put(meta.duress.pinHash)
            } else {
                buf.put(ByteArray(16))
                buf.put(ByteArray(16))
                buf.put(ByteArray(32))
            }
            buf.putInt(meta.failCount)
            buf.putLong(meta.cooldownExpiryEpochMs)

            // Atomic write: write to tmp then rename
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeBytes(buf.array())
            check(tmp.renameTo(file)) { "atomic rename failed" }
        }

        fun read(file: File): AuthMeta {
            val bytes = file.readBytes()
            check(bytes.size == FILE_SIZE) {
                "auth_meta.bin has wrong length: got ${bytes.size}, expected $FILE_SIZE"
            }
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val magic = buf.int
            check(magic == MAGIC) {
                "auth_meta.bin has bad magic: got %08x".format(magic)
            }
            val version = buf.get()
            check(version == VERSION) { "unsupported version: $version" }
            val hasDuress = buf.get() == 1.toByte()
            val duressMode = DuressMode.fromByte(buf.get())

            val keySalt = ByteArray(16).also { buf.get(it) }
            val pinHashSalt = ByteArray(16).also { buf.get(it) }
            val pinHash = ByteArray(32).also { buf.get(it) }

            val duressKeySalt = ByteArray(16).also { buf.get(it) }
            val duressPinHashSalt = ByteArray(16).also { buf.get(it) }
            val duressPinHash = ByteArray(32).also { buf.get(it) }

            val failCount = buf.int
            val cooldownExpiryEpochMs = buf.long

            val duress = if (hasDuress) {
                Duress(duressKeySalt, duressPinHashSalt, duressPinHash, duressMode)
            } else {
                null
            }
            return AuthMeta(keySalt, pinHashSalt, pinHash, duress, failCount, cooldownExpiryEpochMs)
        }
    }

    // equals/hashCode for byte-array fields
    override fun equals(other: Any?): Boolean = error("Compare fields explicitly")
    override fun hashCode(): Int = error("Hashing arrays of mutable bytes is unsafe")
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.crypto.AuthMetaTest"
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/crypto/AuthMeta.kt \
        app/src/test/java/com/hayate0726/tides/crypto/AuthMetaTest.kt
git commit -m "feat(crypto): add AuthMeta read/write for auth_meta.bin"
```

---

## Task 8: KeystoreWrapper (biometric-bound key wrap/unwrap)

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/crypto/KeystoreWrapper.kt`
- Create: `app/src/androidTest/java/com/hayate0726/tides/crypto/KeystoreWrapperTest.kt`

This task requires the Android Keystore so unit-testing on the JVM isn't feasible — we use instrumented tests.

- [ ] **Step 1: Write the failing instrumented test**

Create `app/src/androidTest/java/com/hayate0726/tides/crypto/KeystoreWrapperTest.kt`:

```kotlin
package com.hayate0726.tides.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreWrapperTest {

    @After
    fun cleanup() {
        runCatching { KeystoreWrapper.deleteKey(NON_BIOMETRIC_ALIAS) }
    }

    @Test
    fun wrap_and_unwrap_round_trips_a_32_byte_key() {
        val original = ByteArray(32) { (it + 1).toByte() }
        val wrapped = KeystoreWrapper.wrap(
            alias = NON_BIOMETRIC_ALIAS,
            requireBiometric = false,
            plaintext = original,
        )
        val unwrapped = KeystoreWrapper.unwrap(NON_BIOMETRIC_ALIAS, wrapped)
        assertArrayEquals(original, unwrapped)
    }

    @Test
    fun wrap_with_different_aliases_produces_different_outputs() {
        val plaintext = ByteArray(32) { 7 }
        val w1 = KeystoreWrapper.wrap(NON_BIOMETRIC_ALIAS, false, plaintext)
        runCatching { KeystoreWrapper.deleteKey(SECONDARY_ALIAS) }
        val w2 = KeystoreWrapper.wrap(SECONDARY_ALIAS, false, plaintext)
        assertNotEquals(w1.toList(), w2.toList())
        runCatching { KeystoreWrapper.deleteKey(SECONDARY_ALIAS) }
    }

    @Test
    fun deleteKey_removes_the_alias() {
        KeystoreWrapper.wrap(NON_BIOMETRIC_ALIAS, false, ByteArray(32) { 1 })
        KeystoreWrapper.deleteKey(NON_BIOMETRIC_ALIAS)
        assertFalse(KeystoreWrapper.aliasExists(NON_BIOMETRIC_ALIAS))
    }

    companion object {
        private const val NON_BIOMETRIC_ALIAS = "tides.test.non_biometric"
        private const val SECONDARY_ALIAS = "tides.test.secondary"
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.crypto.KeystoreWrapperTest"
```

Expected: FAIL with "unresolved reference: KeystoreWrapper"

- [ ] **Step 3: Implement `KeystoreWrapper`**

Create `app/src/main/java/com/hayate0726/tides/crypto/KeystoreWrapper.kt`:

```kotlin
package com.hayate0726.tides.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps and unwraps short byte arrays (the DB key) using a key held in
 * Android Keystore. Uses AES-256-GCM. Output format: 12-byte IV || ciphertext+tag.
 *
 * Two flavors:
 *  - `requireBiometric = true`  → wrapping key requires BiometricPrompt to unlock
 *  - `requireBiometric = false` → wrapping key is bound to this app on this device,
 *    no per-use auth (used for "Just for me" preset).
 *
 * The wrapping key itself never leaves the Keystore. We never see it.
 */
object KeystoreWrapper {

    private const val PROVIDER = "AndroidKeyStore"
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun wrap(
        alias: String,
        requireBiometric: Boolean,
        plaintext: ByteArray,
    ): ByteArray {
        val key = getOrCreateKey(alias, requireBiometric)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(plaintext)
        val iv = cipher.iv
        check(iv.size == IV_BYTES) { "expected $IV_BYTES-byte IV, got ${iv.size}" }
        return ByteBuffer.allocate(IV_BYTES + ciphertext.size)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    fun unwrap(alias: String, wrapped: ByteArray): ByteArray {
        require(wrapped.size > IV_BYTES) { "wrapped data too short" }
        val key = loadKey(alias)
            ?: throw IllegalStateException("no Keystore entry for alias $alias")
        val iv = wrapped.copyOfRange(0, IV_BYTES)
        val ciphertext = wrapped.copyOfRange(IV_BYTES, wrapped.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun aliasExists(alias: String): Boolean {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return ks.containsAlias(alias)
    }

    fun deleteKey(alias: String) {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (ks.containsAlias(alias)) ks.deleteEntry(alias)
    }

    private fun getOrCreateKey(alias: String, requireBiometric: Boolean): SecretKey {
        loadKey(alias)?.let { return it }
        val gen = KeyGenerator.getInstance(ALGORITHM, PROVIDER)
        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (requireBiometric) {
            specBuilder.setUserAuthenticationRequired(true)
            // Auth-per-use: the wrapped key cannot be used unless biometric
            // (or device credential fallback) has authenticated within this op.
            specBuilder.setInvalidatedByBiometricEnrollment(true)
        }
        gen.init(specBuilder.build())
        return gen.generateKey()
    }

    private fun loadKey(alias: String): SecretKey? {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return ks.getKey(alias, null) as? SecretKey
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.crypto.KeystoreWrapperTest"
```

Expected: 3 tests pass. (Requires a running emulator or device.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/crypto/KeystoreWrapper.kt \
        app/src/androidTest/java/com/hayate0726/tides/crypto/KeystoreWrapperTest.kt
git commit -m "feat(crypto): add KeystoreWrapper (AES-256-GCM wrap/unwrap via Keystore)"
```

---

## Task 9: SQLCipher-backed Room database with placeholder entity

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/data/Placeholder.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/TidesDatabase.kt`
- Create: `app/src/main/java/com/hayate0726/tides/data/DatabaseFactory.kt`
- Create: `app/src/androidTest/java/com/hayate0726/tides/data/DatabaseFactoryTest.kt`

Plan 2 introduces the real schema. Here we only prove the SQLCipher roundtrip works with a single placeholder entity.

- [ ] **Step 1: Write the failing instrumented test**

Create `app/src/androidTest/java/com/hayate0726/tides/data/DatabaseFactoryTest.kt`:

```kotlin
package com.hayate0726.tides.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseFactoryTest {

    private lateinit var ctx: Context
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        dbFile = File(ctx.filesDir, "test_roundtrip.db")
        dbFile.delete()
    }

    @After
    fun tearDown() {
        dbFile.delete()
    }

    @Test
    fun create_and_reopen_with_same_key_succeeds() = runBlocking {
        val key = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        val db = DatabaseFactory.open(ctx, dbFile, key)
        db.placeholderDao().insert(Placeholder(id = 1, payload = "hello"))
        db.close()
        key.zero()

        val key2 = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        val db2 = DatabaseFactory.open(ctx, dbFile, key2)
        val rows = db2.placeholderDao().all()
        assertEquals(1, rows.size)
        assertEquals("hello", rows[0].payload)
        db2.close()
        key2.zero()
    }

    @Test
    fun reopen_with_wrong_key_fails() = runBlocking {
        val key = KeyDerivation.deriveKey(Pin("123456".toCharArray()), ByteArray(16) { 1 })
        DatabaseFactory.open(ctx, dbFile, key).also {
            it.placeholderDao().insert(Placeholder(id = 1, payload = "data"))
            it.close()
        }
        key.zero()

        val wrongKey = KeyDerivation.deriveKey(Pin("999999".toCharArray()), ByteArray(16) { 1 })
        try {
            val db2 = DatabaseFactory.open(ctx, dbFile, wrongKey)
            // Trying any operation should fail with the wrong key
            db2.placeholderDao().all()
            db2.close()
            fail("expected SQLCipher to reject wrong key")
        } catch (e: Exception) {
            assertNotNull(e)
        } finally {
            wrongKey.zero()
        }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.data.DatabaseFactoryTest"
```

Expected: FAIL with "unresolved reference: DatabaseFactory" or similar.

- [ ] **Step 3: Implement placeholder entity + DAO**

Create `app/src/main/java/com/hayate0726/tides/data/Placeholder.kt`:

```kotlin
package com.hayate0726.tides.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Placeholder entity used by Plan 1 to verify SQLCipher integration.
 * Plan 2 replaces this with the real schema.
 */
@Entity(tableName = "placeholder")
data class Placeholder(
    @PrimaryKey val id: Int,
    val payload: String,
)

@Dao
interface PlaceholderDao {
    @Insert
    suspend fun insert(row: Placeholder)

    @Query("SELECT * FROM placeholder")
    suspend fun all(): List<Placeholder>
}
```

- [ ] **Step 4: Implement `TidesDatabase`**

Create `app/src/main/java/com/hayate0726/tides/data/TidesDatabase.kt`:

```kotlin
package com.hayate0726.tides.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Placeholder::class],
    version = 1,
    exportSchema = true,
)
abstract class TidesDatabase : RoomDatabase() {
    abstract fun placeholderDao(): PlaceholderDao
}
```

- [ ] **Step 5: Implement `DatabaseFactory`**

Create `app/src/main/java/com/hayate0726/tides/data/DatabaseFactory.kt`:

```kotlin
package com.hayate0726.tides.data

import android.content.Context
import androidx.room.Room
import com.hayate0726.tides.crypto.DbKey
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Opens a Room database backed by SQLCipher, using the given `DbKey` to
 * encrypt the on-disk file. The factory does not retain the key — once
 * the database is open, the caller may `zero()` the key.
 *
 * The caller is responsible for closing the database when the app locks.
 */
object DatabaseFactory {

    init {
        // One-time native lib load required by the new sqlcipher-android artifact.
        System.loadLibrary("sqlcipher")
    }

    fun open(
        ctx: Context,
        file: File,
        key: DbKey,
    ): TidesDatabase {
        // SupportOpenHelperFactory copies the passphrase out of the array
        // and then we can let our caller zero theirs.
        val passphrase = key.bytes.copyOf()
        val factory = SupportOpenHelperFactory(passphrase, null, false)
        return Room.databaseBuilder(
            ctx.applicationContext,
            TidesDatabase::class.java,
            file.absolutePath,
        )
            .openHelperFactory(factory)
            .build()
            .also {
                // Note: SupportOpenHelperFactory internally holds the passphrase
                // until the first connection is opened. We let it manage its
                // own zeroing per the sqlcipher-android contract.
            }
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.data.DatabaseFactoryTest"
```

Expected: 2 tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/data app/src/androidTest/java/com/hayate0726/tides/data
git commit -m "feat(data): SQLCipher-backed Room database with placeholder entity"
```

---

## Task 10: Security test — key bytes never on disk

**Files:**
- Create: `app/src/androidTest/java/com/hayate0726/tides/security/KeyNotOnDiskTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/androidTest/java/com/hayate0726/tides/security/KeyNotOnDiskTest.kt`:

```kotlin
package com.hayate0726.tides.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.Placeholder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class KeyNotOnDiskTest {

    private lateinit var ctx: Context
    private lateinit var dbFile: File

    @After
    fun cleanup() {
        if (::dbFile.isInitialized) dbFile.delete()
    }

    @Test
    fun no_app_private_file_contains_the_derived_key_bytes() = runBlocking {
        ctx = ApplicationProvider.getApplicationContext()
        dbFile = File(ctx.filesDir, "key_leak_check.db")
        dbFile.delete()

        val key: DbKey = KeyDerivation.deriveKey(
            Pin("123456".toCharArray()),
            ByteArray(16) { 11 }
        )
        val keyBytes = key.bytes.copyOf()

        val db = DatabaseFactory.open(ctx, dbFile, key)
        db.placeholderDao().insert(Placeholder(1, "x"))
        db.close()
        key.zero()

        // Scan every regular file under filesDir and cacheDir
        val roots = listOf(ctx.filesDir, ctx.cacheDir, ctx.dataDir)
        val needle = keyBytes
        for (root in roots) {
            scanFiles(root) { f ->
                val data = f.readBytes()
                assertFalse(
                    "Key bytes found in app-private file: ${f.absolutePath}",
                    contains(data, needle),
                )
            }
        }
    }

    private fun scanFiles(root: File, body: (File) -> Unit) {
        if (!root.exists()) return
        root.walkTopDown().forEach { f ->
            if (f.isFile && f.length() > 0L) body(f)
        }
    }

    private fun contains(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || haystack.size < needle.size) return false
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return true
        }
        return false
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.security.KeyNotOnDiskTest"
```

Expected: PASS. (We're verifying the invariant; passing is the goal.)

If this fails, it means a file on disk literally contains our 32-byte key — investigate before continuing.

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/hayate0726/tides/security/KeyNotOnDiskTest.kt
git commit -m "test(security): assert derived DB key bytes never appear in app-private files"
```

---

## Task 11: Security test — Logcat never contains sensitive data

**Files:**
- Create: `app/src/androidTest/java/com/hayate0726/tides/security/NoSensitiveLogsTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/androidTest/java/com/hayate0726/tides/security/NoSensitiveLogsTest.kt`:

```kotlin
package com.hayate0726.tides.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.Placeholder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader

@RunWith(AndroidJUnit4::class)
class NoSensitiveLogsTest {

    private val secretMarker = "PIN_MARKER_4f8a9e2b"
    private val flowMarker = "FLOW_MARKER_dead1234"

    @Test
    fun logcat_does_not_leak_pin_or_flow_data() = runBlocking {
        // Clear logcat buffer
        Runtime.getRuntime().exec(arrayOf("logcat", "-c"))

        val ctx: Context = ApplicationProvider.getApplicationContext()
        val dbFile = java.io.File(ctx.filesDir, "log_leak_check.db")
        dbFile.delete()

        val pin = Pin(secretMarker.toCharArray())
        val key = KeyDerivation.deriveKey(pin, ByteArray(16) { 5 })
        pin.zero()

        val db = DatabaseFactory.open(ctx, dbFile, key)
        db.placeholderDao().insert(Placeholder(1, flowMarker))
        db.close()
        key.zero()

        // Read recent logcat
        val proc = Runtime.getRuntime().exec(
            arrayOf("logcat", "-d", "-v", "raw")
        )
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        val all = reader.readText()
        proc.destroy()

        assertFalse(
            "PIN content appeared in logcat",
            all.contains(secretMarker),
        )
        assertFalse(
            "Flow content appeared in logcat",
            all.contains(flowMarker),
        )

        dbFile.delete()
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.security.NoSensitiveLogsTest"
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/hayate0726/tides/security/NoSensitiveLogsTest.kt
git commit -m "test(security): assert sensitive content never appears in Logcat"
```

---

## Task 12: LockManager — encapsulate lock state and rate-limit

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/lock/LockState.kt`
- Create: `app/src/main/java/com/hayate0726/tides/lock/LockManager.kt`
- Create: `app/src/test/java/com/hayate0726/tides/lock/LockManagerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/hayate0726/tides/lock/LockManagerTest.kt`:

```kotlin
package com.hayate0726.tides.lock

import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong

class LockManagerTest {

    private val nowMs = AtomicLong(1_000_000L)
    private val clock = LockManager.Clock { nowMs.get() }

    private val keySalt = ByteArray(16) { 1 }
    private val pinHashSalt = ByteArray(16) { 2 }
    private val correctPin = "654321"
    private val correctHash = KeyDerivation.derivePinHash(
        Pin(correctPin.toCharArray()), pinHashSalt
    )

    private val baseMeta = AuthMeta(
        keySalt = keySalt,
        pinHashSalt = pinHashSalt,
        pinHash = correctHash,
        duress = null,
        failCount = 0,
        cooldownExpiryEpochMs = 0L,
    )

    @Test
    fun `correct PIN unlocks and resets fail count`() = runTest {
        val store = InMemoryAuthMetaStore(baseMeta)
        val mgr = LockManager(store, clock)
        val result = mgr.attemptUnlock(Pin(correctPin.toCharArray()))
        assertTrue(result is LockManager.UnlockResult.Success)
        assertEquals(0, store.current().failCount)
    }

    @Test
    fun `wrong PIN increments fail count`() = runTest {
        val store = InMemoryAuthMetaStore(baseMeta)
        val mgr = LockManager(store, clock)
        repeat(3) {
            mgr.attemptUnlock(Pin("000000".toCharArray()))
        }
        assertEquals(3, store.current().failCount)
    }

    @Test
    fun `5th wrong PIN triggers cooldown`() = runTest {
        val store = InMemoryAuthMetaStore(baseMeta)
        val mgr = LockManager(store, clock)
        repeat(5) {
            mgr.attemptUnlock(Pin("000000".toCharArray()))
        }
        val meta = store.current()
        assertEquals(5, meta.failCount)
        assertEquals(nowMs.get() + 30_000L, meta.cooldownExpiryEpochMs)
    }

    @Test
    fun `unlock during cooldown returns RateLimited even with correct PIN`() = runTest {
        val store = InMemoryAuthMetaStore(
            baseMeta.copy(failCount = 5, cooldownExpiryEpochMs = nowMs.get() + 10_000L)
        )
        val mgr = LockManager(store, clock)
        val r = mgr.attemptUnlock(Pin(correctPin.toCharArray()))
        assertTrue(r is LockManager.UnlockResult.RateLimited)
    }

    @Test
    fun `cooldown doubles each subsequent batch up to 1 hour cap`() = runTest {
        val store = InMemoryAuthMetaStore(baseMeta)
        val mgr = LockManager(store, clock)
        // 1st batch
        repeat(5) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        assertEquals(30_000L, store.current().cooldownExpiryEpochMs - nowMs.get())
        nowMs.set(nowMs.get() + 31_000L) // expire cooldown

        repeat(5) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        assertEquals(60_000L, store.current().cooldownExpiryEpochMs - nowMs.get())
        nowMs.set(nowMs.get() + 61_000L)

        repeat(5) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        assertEquals(120_000L, store.current().cooldownExpiryEpochMs - nowMs.get())
    }

    @Test
    fun `cap is 1 hour regardless of fail count growth`() = runTest {
        val store = InMemoryAuthMetaStore(
            baseMeta.copy(failCount = 100, cooldownExpiryEpochMs = 0L)
        )
        val mgr = LockManager(store, clock)
        repeat(5) { mgr.attemptUnlock(Pin("000000".toCharArray())) }
        val gap = store.current().cooldownExpiryEpochMs - nowMs.get()
        assertEquals(3_600_000L, gap)
    }

    private class InMemoryAuthMetaStore(initial: AuthMeta) : LockManager.AuthMetaStore {
        private var state = initial
        fun current() = state
        override fun load(): AuthMeta = state
        override fun update(updater: (AuthMeta) -> AuthMeta) {
            state = updater(state)
        }
    }
}

private fun AuthMeta.copy(
    failCount: Int = this.failCount,
    cooldownExpiryEpochMs: Long = this.cooldownExpiryEpochMs,
) = AuthMeta(
    keySalt = this.keySalt,
    pinHashSalt = this.pinHashSalt,
    pinHash = this.pinHash,
    duress = this.duress,
    failCount = failCount,
    cooldownExpiryEpochMs = cooldownExpiryEpochMs,
)
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.lock.LockManagerTest"
```

Expected: FAIL with "unresolved reference: LockManager"

- [ ] **Step 3: Implement `LockState` and `LockManager`**

Create `app/src/main/java/com/hayate0726/tides/lock/LockState.kt`:

```kotlin
package com.hayate0726.tides.lock

import com.hayate0726.tides.crypto.DbKey

sealed interface LockState {
    data object Locked : LockState
    data object UnlockingPin : LockState
    data object UnlockingBiometric : LockState
    data class Unlocked(val key: DbKey) : LockState
    data class LockedCooldown(val expiryEpochMs: Long) : LockState
    data object DuressDecoy : LockState
    data object DuressWipe : LockState
}
```

Create `app/src/main/java/com/hayate0726/tides/lock/LockManager.kt`:

```kotlin
package com.hayate0726.tides.lock

import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin

/**
 * Stateless logic for PIN attempts. The UI layer feeds in PIN entries
 * and gets back UnlockResult. Rate-limit and cooldown are enforced here.
 *
 * Lock state (the StateFlow) lives in a ViewModel in Plan 3.
 */
class LockManager(
    private val store: AuthMetaStore,
    private val clock: Clock,
) {

    interface Clock {
        fun nowMs(): Long
    }

    interface AuthMetaStore {
        fun load(): AuthMeta
        fun update(updater: (AuthMeta) -> AuthMeta)
    }

    sealed interface UnlockResult {
        data class Success(val key: DbKey) : UnlockResult
        data object WrongPin : UnlockResult
        data object Duress : UnlockResult
        data class RateLimited(val expiryEpochMs: Long) : UnlockResult
    }

    /**
     * Try to unlock with the given PIN. Caller is responsible for zeroing
     * the PIN after this call returns.
     */
    fun attemptUnlock(pin: Pin): UnlockResult {
        val meta = store.load()
        val now = clock.nowMs()

        if (meta.cooldownExpiryEpochMs > now) {
            return UnlockResult.RateLimited(meta.cooldownExpiryEpochMs)
        }

        // Check primary first
        if (KeyDerivation.validatePin(pin, meta.pinHashSalt, meta.pinHash)) {
            val key = KeyDerivation.deriveKey(pin, meta.keySalt)
            store.update { it.copyWith(failCount = 0, cooldownExpiryEpochMs = 0L) }
            return UnlockResult.Success(key)
        }

        // Check duress if configured
        val duress = meta.duress
        if (duress != null &&
            KeyDerivation.validatePin(pin, duress.pinHashSalt, duress.pinHash)
        ) {
            return UnlockResult.Duress
        }

        // Wrong PIN
        val newFailCount = meta.failCount + 1
        val batchPosition = newFailCount % FAIL_BATCH_SIZE
        val newCooldownExpiry = if (batchPosition == 0) {
            val batchNumber = newFailCount / FAIL_BATCH_SIZE
            val cooldownMs = computeCooldownMs(batchNumber)
            now + cooldownMs
        } else {
            meta.cooldownExpiryEpochMs
        }
        store.update {
            it.copyWith(failCount = newFailCount, cooldownExpiryEpochMs = newCooldownExpiry)
        }
        return UnlockResult.WrongPin
    }

    private fun computeCooldownMs(batchNumber: Int): Long {
        // 30s, 60s, 120s, 240s, ..., capped at 1 hour
        val base = 30_000L
        val scaled = base shl (batchNumber - 1).coerceAtLeast(0)
        return scaled.coerceAtMost(3_600_000L)
    }

    companion object {
        const val FAIL_BATCH_SIZE = 5
    }
}

private fun AuthMeta.copyWith(
    failCount: Int = this.failCount,
    cooldownExpiryEpochMs: Long = this.cooldownExpiryEpochMs,
): AuthMeta = AuthMeta(
    keySalt = this.keySalt,
    pinHashSalt = this.pinHashSalt,
    pinHash = this.pinHash,
    duress = this.duress,
    failCount = failCount,
    cooldownExpiryEpochMs = cooldownExpiryEpochMs,
)
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.lock.LockManagerTest"
```

Expected: 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/lock app/src/test/java/com/hayate0726/tides/lock
git commit -m "feat(lock): add LockManager with rate-limit and duress detection"
```

---

## Task 13: Security test — wrong PIN brute force

**Files:**
- Create: `app/src/androidTest/java/com/hayate0726/tides/security/WrongPinBruteForceTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/androidTest/java/com/hayate0726/tides/security/WrongPinBruteForceTest.kt`:

```kotlin
package com.hayate0726.tides.security

import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.lock.LockManager
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WrongPinBruteForceTest {

    @Test
    fun no_wrong_pin_unlocks() {
        val pinHashSalt = ByteArray(16) { 9 }
        val correctPin = "847291"
        val correctHash = KeyDerivation.derivePinHash(Pin(correctPin.toCharArray()), pinHashSalt)
        val meta = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = pinHashSalt,
            pinHash = correctHash,
            duress = null,
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )

        val store = object : LockManager.AuthMetaStore {
            private var state = meta
            override fun load() = state
            override fun update(updater: (AuthMeta) -> AuthMeta) {
                state = updater(state)
            }
        }
        val mgr = LockManager(store, object : LockManager.Clock { override fun nowMs() = 0L })

        val rng = Random(seed = 42)
        repeat(1000) {
            var candidate = rng.nextInt(0, 1_000_000).toString().padStart(6, '0')
            if (candidate == correctPin) candidate = "000000" // avoid accidental match
            // Bypass cooldown by resetting state — we're testing brute force not rate-limit here
            store.update { it.let { m ->
                AuthMeta(m.keySalt, m.pinHashSalt, m.pinHash, m.duress, 0, 0L)
            } }
            val r = mgr.attemptUnlock(Pin(candidate.toCharArray()))
            assertTrue(
                "Wrong PIN '$candidate' was reported as Success",
                r !is LockManager.UnlockResult.Success && r !is LockManager.UnlockResult.Duress
            )
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.security.WrongPinBruteForceTest"
```

Expected: PASS. (Note: this is a unit test, runs on JVM with Argon2 native libs. May take 30–90 seconds because 1000 × Argon2id.)

If runtime is excessive (>3 minutes), reduce the iteration count to 200 and document the tradeoff in a comment.

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/hayate0726/tides/security/WrongPinBruteForceTest.kt
git commit -m "test(security): brute-force 1000 wrong PINs and verify none unlock"
```

---

## Task 14: Security test — rate-limit holds under repeated attempts

**Files:**
- Create: `app/src/androidTest/java/com/hayate0726/tides/security/RateLimitTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/androidTest/java/com/hayate0726/tides/security/RateLimitTest.kt`:

```kotlin
package com.hayate0726.tides.security

import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.lock.LockManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimitTest {

    @Test
    fun five_wrong_attempts_then_cooldown_blocks_correct_pin() {
        val correctPin = "112233"
        val pinHashSalt = ByteArray(16) { 7 }
        val pinHash = KeyDerivation.derivePinHash(Pin(correctPin.toCharArray()), pinHashSalt)
        val meta = AuthMeta(
            keySalt = ByteArray(16),
            pinHashSalt = pinHashSalt,
            pinHash = pinHash,
            duress = null,
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )

        var currentMeta = meta
        val store = object : LockManager.AuthMetaStore {
            override fun load() = currentMeta
            override fun update(updater: (AuthMeta) -> AuthMeta) { currentMeta = updater(currentMeta) }
        }
        val nowHolder = longArrayOf(1_000_000L)
        val clock = object : LockManager.Clock { override fun nowMs() = nowHolder[0] }
        val mgr = LockManager(store, clock)

        repeat(5) {
            val r = mgr.attemptUnlock(Pin("000000".toCharArray()))
            assertTrue(r is LockManager.UnlockResult.WrongPin)
        }
        assertEquals(5, currentMeta.failCount)
        assertEquals(nowHolder[0] + 30_000L, currentMeta.cooldownExpiryEpochMs)

        // Correct PIN during cooldown must be rejected
        val r = mgr.attemptUnlock(Pin(correctPin.toCharArray()))
        assertTrue(
            "Correct PIN unlocked during cooldown — rate-limit broken",
            r is LockManager.UnlockResult.RateLimited,
        )

        // Advance clock past cooldown; correct PIN now works
        nowHolder[0] += 31_000L
        val r2 = mgr.attemptUnlock(Pin(correctPin.toCharArray()))
        assertTrue(r2 is LockManager.UnlockResult.Success)
    }
}
```

- [ ] **Step 2: Run the test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.hayate0726.tides.security.RateLimitTest"
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/hayate0726/tides/security/RateLimitTest.kt
git commit -m "test(security): assert rate-limit blocks correct PIN during cooldown"
```

---

## Task 15: AuthMetaStore wiring (file-backed implementation)

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/crypto/FileAuthMetaStore.kt`
- Create: `app/src/androidTest/java/com/hayate0726/tides/crypto/FileAuthMetaStoreTest.kt`

- [ ] **Step 1: Write the failing instrumented test**

Create `app/src/androidTest/java/com/hayate0726/tides/crypto/FileAuthMetaStoreTest.kt`:

```kotlin
package com.hayate0726.tides.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.lock.LockManager
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileAuthMetaStoreTest {

    private lateinit var ctx: Context
    private lateinit var file: File

    @After
    fun tearDown() { if (::file.isInitialized) file.delete() }

    @Test
    fun load_throws_when_file_missing() {
        ctx = ApplicationProvider.getApplicationContext()
        file = File(ctx.filesDir, "missing_auth_meta.bin")
        file.delete()
        val store = FileAuthMetaStore(file)
        try {
            store.load()
            assert(false) { "expected throw" }
        } catch (e: Exception) {
            // ok
        }
    }

    @Test
    fun initialize_creates_file_with_correct_data() {
        ctx = ApplicationProvider.getApplicationContext()
        file = File(ctx.filesDir, "init_auth_meta.bin")
        file.delete()
        val store = FileAuthMetaStore(file)

        val meta = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = ByteArray(16) { 2 },
            pinHash = ByteArray(32) { 3 },
            duress = null,
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )
        store.initialize(meta)
        val read = store.load()
        assertArrayEquals(meta.pinHash, read.pinHash)
    }

    @Test
    fun update_writes_atomically_and_can_be_re_read() {
        ctx = ApplicationProvider.getApplicationContext()
        file = File(ctx.filesDir, "update_auth_meta.bin")
        file.delete()
        val store = FileAuthMetaStore(file)

        store.initialize(
            AuthMeta(
                keySalt = ByteArray(16),
                pinHashSalt = ByteArray(16),
                pinHash = ByteArray(32),
                duress = null,
                failCount = 0,
                cooldownExpiryEpochMs = 0L,
            )
        )
        store.update { it.let { m ->
            AuthMeta(m.keySalt, m.pinHashSalt, m.pinHash, m.duress, 7, 999L)
        } }
        val read = store.load()
        assertEquals(7, read.failCount)
        assertEquals(999L, read.cooldownExpiryEpochMs)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.crypto.FileAuthMetaStoreTest"
```

Expected: FAIL with "unresolved reference: FileAuthMetaStore"

- [ ] **Step 3: Implement `FileAuthMetaStore`**

Create `app/src/main/java/com/hayate0726/tides/crypto/FileAuthMetaStore.kt`:

```kotlin
package com.hayate0726.tides.crypto

import com.hayate0726.tides.lock.LockManager
import java.io.File

/**
 * File-backed [LockManager.AuthMetaStore]. The file is `auth_meta.bin` in
 * the app-private files directory. Reads are direct; writes go through
 * AuthMeta.write which performs atomic-rename.
 *
 * Thread-safety: all updates synchronize on the receiver. Reads are
 * lock-free but may briefly see a stale value during an update.
 */
class FileAuthMetaStore(private val file: File) : LockManager.AuthMetaStore {

    /** Write the initial meta — used by onboarding. */
    fun initialize(meta: AuthMeta) {
        synchronized(this) {
            AuthMeta.write(file, meta)
        }
    }

    override fun load(): AuthMeta = AuthMeta.read(file)

    override fun update(updater: (AuthMeta) -> AuthMeta) {
        synchronized(this) {
            val current = AuthMeta.read(file)
            val next = updater(current)
            AuthMeta.write(file, next)
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.hayate0726.tides.crypto.FileAuthMetaStoreTest"
```

Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/crypto/FileAuthMetaStore.kt \
        app/src/androidTest/java/com/hayate0726/tides/crypto/FileAuthMetaStoreTest.kt
git commit -m "feat(crypto): add FileAuthMetaStore (file-backed AuthMeta persistence)"
```

---

## Task 16: Hilt wiring

**Files:**
- Create: `app/src/main/java/com/hayate0726/tides/di/CryptoModule.kt`
- Create: `app/src/main/java/com/hayate0726/tides/di/DataModule.kt`

- [ ] **Step 1: Create `CryptoModule`**

Create `app/src/main/java/com/hayate0726/tides/di/CryptoModule.kt`:

```kotlin
package com.hayate0726.tides.di

import android.content.Context
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.lock.LockManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides
    @Singleton
    fun provideAuthMetaFile(@ApplicationContext ctx: Context): File =
        File(ctx.filesDir, "auth_meta.bin")

    @Provides
    @Singleton
    fun provideAuthMetaStore(file: File): FileAuthMetaStore = FileAuthMetaStore(file)

    @Provides
    @Singleton
    fun provideClock(): LockManager.Clock = LockManager.Clock { System.currentTimeMillis() }

    @Provides
    @Singleton
    fun provideLockManager(
        store: FileAuthMetaStore,
        clock: LockManager.Clock,
    ): LockManager = LockManager(store, clock)
}
```

- [ ] **Step 2: Create `DataModule` (database factory wiring; DB instance created on unlock, not at startup)**

Create `app/src/main/java/com/hayate0726/tides/di/DataModule.kt`:

```kotlin
package com.hayate0726.tides.di

import android.content.Context
import com.hayate0726.tides.data.DatabaseFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CyclesDbFile

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @CyclesDbFile
    fun provideCyclesDbFile(@ApplicationContext ctx: Context): File =
        File(ctx.filesDir, "cycles.db")

    @Provides
    @Singleton
    fun provideDatabaseFactory(): DatabaseFactory.Companion = DatabaseFactory
}
```

- [ ] **Step 3: Build to verify wiring compiles**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/hayate0726/tides/di
git commit -m "feat(di): wire CryptoModule and DataModule via Hilt"
```

---

## Task 17: CI — GitHub Actions workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create the workflow**

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  pull_request:
  push:
    branches: [main]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4

      - name: Build debug APK
        run: ./gradlew :app:assembleDebug

      - name: Run unit tests
        run: ./gradlew :app:testDebugUnitTest

      - name: Verify APK does not declare INTERNET
        run: |
          APK=$(find app/build/outputs/apk/debug -name "*.apk" | head -n 1)
          echo "Auditing $APK"
          # aapt2 ships with the Android SDK Build Tools; the Gradle build pulls it in.
          $ANDROID_HOME/build-tools/34.0.0/aapt2 dump permissions "$APK" > perms.txt
          cat perms.txt
          if grep -q "android.permission.INTERNET" perms.txt; then
            echo "::error::INTERNET permission found in APK"
            exit 1
          fi
          if grep -q "android.permission.ACCESS_NETWORK_STATE" perms.txt; then
            echo "::error::ACCESS_NETWORK_STATE found in APK"
            exit 1
          fi
          echo "OK: no network permissions"

  instrumented-tests:
    # Connected tests run on a managed device. Optional in PR — required on release.
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: Enable KVM
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' \
            | sudo tee /etc/udev/rules.d/99-kvm4all.rules
          sudo udevadm control --reload-rules
          sudo udevadm trigger --name-match=kvm
      - uses: gradle/actions/setup-gradle@v4
      - name: Run instrumented tests on emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          target: google_apis
          arch: x86_64
          script: ./gradlew :app:connectedDebugAndroidTest
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions workflow with INTERNET permission audit"
```

---

## Task 18: README and CHANGELOG seeds

**Files:**
- Create: `README.md`
- Create: `CHANGELOG.md`
- Create: `LICENSE` (GPL-3.0)

- [ ] **Step 1: Create `LICENSE` (GPL-3.0)**

Download the official text and save as `LICENSE`:

```bash
curl -sSL https://www.gnu.org/licenses/gpl-3.0.txt -o LICENSE
```

If offline, copy the official GPL-3.0 text from any other GPL-3.0 project. The file must be the full unmodified license.

- [ ] **Step 2: Create `README.md`**

Create `README.md`:

```markdown
# Tides

An offline-first period tracker for Android. Free, open source, no accounts, no telemetry, no network.

**Status:** in development. v0.1.0-dev. Not yet ready for users.

## Design

See [docs/superpowers/specs/2026-05-15-tides-design.md](docs/superpowers/specs/2026-05-15-tides-design.md) for the full design spec.

Plans live in [docs/superpowers/plans/](docs/superpowers/plans/).

## Privacy

- The app's manifest does not declare `INTERNET`. The app cannot make a network call.
- All cycle data is stored in a single SQLCipher-encrypted database on the device.
- The encryption key is derived from your PIN via Argon2id and stored in the Android Keystore (biometric-bound if you enable biometric unlock).
- There is no account, no telemetry, no analytics, no cloud sync.

## License

GPL-3.0. See [LICENSE](LICENSE).
```

- [ ] **Step 3: Create `CHANGELOG.md`**

Create `CHANGELOG.md`:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added — Plan 1: Foundation & Crypto
- Project scaffold: Gradle, Hilt, Compose, Room + SQLCipher
- `crypto/` package: Argon2id wrapper, key derivation, Keystore wrap/unwrap, `auth_meta.bin` format
- `LockManager` with rate-limit and duress detection
- Security tests: no INTERNET permission, key not on disk, no sensitive logs, wrong-PIN brute force, rate-limit enforcement
- GitHub Actions CI with APK permission audit

### Privacy/security changes
- Manifest does not declare `INTERNET`. Static check in CI enforces this.
```

- [ ] **Step 4: Commit**

```bash
git add README.md CHANGELOG.md LICENSE
git commit -m "docs: add README, CHANGELOG, GPL-3.0 LICENSE"
```

---

## Plan 1 acceptance criteria

Before marking Plan 1 complete and moving to Plan 2:

- [ ] `./gradlew :app:assembleDebug` succeeds
- [ ] `./gradlew :app:testDebugUnitTest` — all unit tests pass (Argon2, KeyDerivation, AuthMeta, DbKeyZeroing, LockManager, WrongPinBruteForce, RateLimit)
- [ ] `./gradlew :app:connectedDebugAndroidTest` (against an emulator or real Android 14 device) — all instrumented tests pass (KeystoreWrapper, FileAuthMetaStore, DatabaseFactory, NoInternetPermission, KeyNotOnDisk, NoSensitiveLogs)
- [ ] The APK permissions audit script in CI finds no `INTERNET` or `ACCESS_NETWORK_STATE`
- [ ] All commits are atomic and follow the conventional-commits style
- [ ] CHANGELOG.md updated under "Plan 1: Foundation & Crypto"

What Plan 1 does **not** produce:
- Onboarding UI (Plan 3)
- Real Room schema (Plan 2)
- Any user-facing functionality beyond a static "foundation layer" screen

The placeholder entity in `data/Placeholder.kt` is removed in Plan 2 when the real schema lands.
