# PocketHost

PocketHost is an Android app that serves a selected static website folder from the phone.


## Current build contents
- SAF folder picker with persisted read access
- Foreground local web server on port 8080
- Localhost and LAN URL display
- Case-insensitive website file lookup (e.g. `Index.html`/`index.html`)
- Nested folders and common static assets
- Path traversal checks
- Basic live request count, uptime, and last-request dashboard
- Copy LAN URL button
- GitHub Actions cloud debug APK build

## Cloud build
Push to `main` or manually run the workflow from Actions. Download the `PocketHost-debug-apk` artifact.


## Current limitations
- Static file hosting only; no PHP, Node runtime, databases, or server-side scripts.
- Public Cloudflare Tunnel, custom-domain mapping, multi-site management, QR code, file manager/editor, access controls, and full request/error logs are future work.
- Android background execution/battery behavior varies by device. Keep the foreground notification visible while hosting.

## Test
1. Select a website folder containing an HTML file. File matching is case-insensitive for the default index name.
2. Start the server.
3. Open `http://127.0.0.1:8080` on the same phone.
4. Open the LAN URL from a second device on the same Wi-Fi.
5. Check the request counter and last-request text in PocketHost.
