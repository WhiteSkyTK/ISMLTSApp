# ISMLTS student app (Android)

[![Android - build, test and lint](https://github.com/WhiteSkyTK/ISMLTSApp/actions/workflows/android.yml/badge.svg)](https://github.com/WhiteSkyTK/ISMLTSApp/actions/workflows/android.yml)

The Android app for students of **ISMLTS**, the Integrated School Management and Learning Tracking System built for
IIE Rosebank College (WIL module XADAD7112, Group 1 ADAD1). It opens the ISMLTS website in a locked-down WebView,
with a native home screen and bottom bar, so every feature of the website (marks, assessments, quizzes, the register,
the calendar, tickets) is in the app the moment it goes live.

The website is a separate repository, [ISMLTS-WebApp-](https://github.com/WhiteSkyTK/ISMLTS-WebApp-). Its
`docs/ANDROID-WEBVIEW.md` explains what the site expects from this app.

## Contents

- [What the app does](#what-the-app-does)
- [How it keeps students safe](#how-it-keeps-students-safe)
- [Project structure](#project-structure)
- [Running it](#running-it)
- [Tests and checks](#tests-and-checks)
- [QR codes that open the app (App Links)](#qr-codes-that-open-the-app-app-links)
- [Releasing](#releasing)
- [Team](#team)

## What the app does

- **Home screen** with tiles for **Scan In**, **Work To Do**, **Announcements**, **My Progress**, **Calendar** and
  **Profile**, and a bottom bar for the pages students use most.
- **Scanning in to class:** the register page gets the phone's location (after the student allows it), so the website
  can check the student is in the classroom. Each phone can sign in only one student per class.
- **Handing in work:** the website's "Upload your work" button opens Android's file picker.
- **Downloads:** files open in the browser (from Azure Storage) or go to the download manager.
- **Calendar link:** "Add to my calendar" opens the phone's calendar app.
- **Register QR codes** scanned with the camera open straight in the app (see App Links below).
- Pull to refresh, a splash screen, a "No connection" screen with **Try again**, and the back button going back a page.
- Text follows the phone's font size setting.

## How it keeps students safe

| What | How |
| --- | --- |
| Only the ISMLTS site runs inside the app | Links to other sites open in the browser; `intent:`, `file:`, `javascript:`, `content:` and `data:` links are ignored (`SiteLinks`). Look-alike addresses such as `ismlts.example.net.evil.com` don't count as the site. |
| https only | `network_security_config.xml` refuses plain http and trusts only the phone's built-in certificates. Certificate errors are never skipped. Debug builds may use http to the PC's own website at `10.0.2.2` for testing. |
| Sign-in cookies stay on the phone | Backups and phone-to-phone transfers are off (`allowBackup="false"` and the backup rules), because the WebView's cookies sign the student in and identify the phone for the register. Third-party cookies are off. |
| Location only for the site | Only the ISMLTS site can ask for the phone's location, only after the student allows it, and only while a page needs it. |
| No access to the phone's files | The WebView can't read files or content providers, and https pages can't load http content. Google Safe Browsing is on. |
| Download cookies don't leak | The download manager only gets the sign-in cookie for downloads from the ISMLTS site itself. |
| Debugging is off in release | Chrome's WebView debugging only works in debug builds, and release builds are shrunk and obfuscated by R8. |

## Project structure

```
app/src/main/java/com/pantech/ismltsapp/
  MainActivity.kt          the one screen: home tiles, bottom bar, and the WebView (wires the classes below)
  config/
    SiteConfig.kt          the site address (from Gradle, see "Running it")
    Destination.kt         the pages the tiles and bottom bar open: path, title and bottom-bar item
  web/
    SiteLinks.kt           which links belong to the site, and what to do with every other link (unit-tested)
    WebViewSetup.kt        the WebView's settings: what the site needs and nothing more
    SiteWebViewClient.kt   page loading, errors, and sending outside links to other apps
    SiteChromeClient.kt    location for scanning in, the file picker, page progress
    SiteDownloads.kt       downloads through Android's download manager
app/src/main/res/          layout, strings, icons, colours, network security and backup rules
app/src/debug/res/xml/     the debug-only network rules (http to the PC's website)
app/src/test/              unit tests (SiteLinksTest)
```

To add a page to the home screen: add an entry to `Destination` (the website path and a title string), add a tile to
`activity_main.xml`, and map the tile's id to the destination in `MainActivity.setupTiles()`.

## Running it

**You need** a recent Android Studio with the Android SDK 37 platform. Gradle runs on JDK 25 and downloads it by itself
if it isn't installed.

1. Open the project folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on an emulator or a phone (USB debugging on).
3. Log in with a student account of the website. (The live site's demo accounts use the demo password set on the
   App Service.)

**Which website the app opens:** the live site by default. To try another one, pass a Gradle property, for example:

```bash
./gradlew installDebug -Pismlts.siteUrl=https://your-test-site.azurewebsites.net
```

To use the website running on your PC with the emulator (debug builds only), start the website with
`dotnet run --launch-profile http`, then:

```bash
./gradlew installDebug -Pismlts.siteUrl=http://10.0.2.2:5269
```

You can also put `ismlts.siteUrl=...` in your own `~/.gradle/gradle.properties` so Android Studio uses it.

## Tests and checks

```bash
./gradlew testDebugUnitTest lintDebug
```

- `SiteLinksTest` covers which links stay in the app, which go to other apps and which are blocked, including
  look-alike hosts, other ports, plain http and `intent:`/`file:`/`javascript:` links.
- Android Lint must pass with no errors.

On a phone, also work through the checklist in the website's `docs/ANDROID-WEBVIEW.md`: location when scanning in, the
same-phone check, uploading and downloading a file, taking a quiz, the calendar link and the back button.

GitHub Actions (`.github/workflows/android.yml`) runs the unit tests, lint and a debug build on every push and pull
request, and keeps the debug APK and the reports for 7 days.

## QR codes that open the app (App Links)

The register's QR code is a link to `https://<site>/Attendance/Scan?code=...`. The app claims those links, so scanning
the code with the camera opens the app instead of Chrome. Android only allows that once the website vouches for the
app:

1. Get the SHA-256 fingerprint of the certificate the app is signed with: in Android Studio run the Gradle task
   `app > Tasks > android > signingReport` (debug builds), or copy it from Play Console > App integrity.
2. On the website's App Service add `AndroidApp__PackageName` = `com.pantech.ismltsapp` and
   `AndroidApp__CertificateFingerprints__0` = the fingerprint (`AB:CD:...`). Add `__1` for a second certificate,
   such as the debug one.
3. Check `https://<site>/.well-known/assetlinks.json` shows them, then reinstall the app.
4. Check Android accepted it: `adb shell pm get-app-links com.pantech.ismltsapp` should say `verified`.

Students can always type the 6-letter code on **Scan In** instead.

## Releasing

1. Raise `versionCode` and `versionName` in `app/build.gradle.kts`.
2. In Android Studio choose **Build > Generate Signed App Bundle or APK** and sign with the team's upload key. Keep the
   key and its passwords out of git (`*.jks`, `*.keystore` and `keystore.properties` are ignored).
3. Add the release key's fingerprint to the website's App Links settings (step 2 above).

## Team

| Name | Student number |
| --- | --- |
| Tokollo Will Nonyane | ST10296818 |
| Phathutshedzo Ramsy Ramakuela | ST10369372 |
| Gundo Mathantshani | ST10367584 |

Lecturer: Edward Nkata · IIE Rosebank College · WIL module XADAD7112, Group 1 (ADAD1).

Branching: feature branch > pull request into `master` > at least one teammate reviews before merging.
