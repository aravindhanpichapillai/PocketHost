# PocketHost Android MVP

A first prototype: choose a folder via Android's Storage Access Framework, then serve static files on port 8080 with NanoHTTPD. Foreground-service hosting is included.

## Build
Open this folder as an Android Gradle project in Android Studio or a compatible Android IDE. Build with Android Gradle Plugin 8.6.1 and JDK 17.

## Test
1. Create a website folder with `index.html` and optional CSS/JS/images.
2. Install/run the app.
3. Choose the folder.
4. Tap START SERVER.
5. Open `http://127.0.0.1:8080` on the phone.
6. On the same Wi-Fi, test the LAN address shown in the app from another device.

## Notes
- This prototype serves static files only.
- The Android foreground service keeps the server more reliable while backgrounded, but device vendors may still apply battery restrictions.
- Cloudflare Tunnel is not included in this first milestone. We'll integrate it in the next iteration.
