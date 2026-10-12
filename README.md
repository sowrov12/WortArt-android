# WortArt (German Article Bangla) — Native Android

Kotlin + Jetpack Compose version of the WortArt web app (der / die / das practice).

## Build with GitHub Actions
1. Unzip and upload **all** files to a GitHub repository (keep the hidden `.github` folder and `app/wortart.keystore`).
2. Open the **Actions** tab. The *Build Android APK* workflow runs on every push (or press *Run workflow*).
3. When it is green, open the run and download **WortArt-apk** under *Artifacts*. Unzip it and install `app-release.apk`.
4. If the run is red, copy the last error lines and send them to Claude.

## Words
Words and category images come live from the same Google Sheet as the website.
A snapshot is bundled (`app/src/main/assets/words_fallback.csv`) for offline use.

## Google login + Premium (optional)
Without these steps the app works fully; only the Premium tab shows "login not set up".
1. Firebase console → your existing project → *Add app → Android*.
   - Package name: `com.sowrov.germanarticlebangla`
   - SHA-1: `E9:F8:71:B0:19:5C:1B:D6:75:60:59:10:2B:6C:4B:46:1B:94:B5:F6`
2. Authentication → Sign-in method → make sure **Google** is enabled.
3. Download `google-services.json` and put it in the `app/` folder, then upload it to the repository.
4. (Admin panel) edit `gradle.properties` and set `ADMIN_EMAIL=your@gmail.com`.
   Keep your Firestore rules as on the website: users may only write their own profile and never `premiumUntil`.

## Notes
- The signing key in `app/wortart.keystore` is a test key kept in the repo so the SHA-1 never changes. Create your own key before publishing on Google Play.
- Reminders need the notification permission (asked when you switch Reminders on).
