# Avatar device regression

This fixture is test-only. It never calls Pandora, uses only fixed fixture identities,
and stores generated image bytes in memory. Real database persistence and permission
boundaries remain covered by backend `avatarUploadsAndServesRealImageBytes`.

1. From the repository root run `python android/test-tools/avatar_fixture_server.py`.
2. Start the existing Android emulator and, from `android/`, run PowerShell:

```powershell
./gradlew.bat :app:connectedAvatarQaAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.pandora6ix.app.AvatarDeviceTest' '-Pandroid.testInstrumentationRunnerArguments.avatarFixtureUrl=http://10.0.2.2:18766/avatar-fixture'
```

3. Stop the Python process after testing. The test APK's sessions are cleared in
   `finally`. The `avatarQa` build uses `com.pandora6ix.app.avatarqa` and the label
   `Pandora Avatar QA`; test-runner installation/cleanup cannot remove the real app.

The test uses actual Android JPEG compression/Bitmap decoding and Compose pixel
capture: 512x320 upload becomes 256x160, a separate authenticated repository shows
the photograph, replacement refreshes the uploader immediately and an existing
reader after cache expiry, and a fresh repository reads replacement bytes.
HTTP/1.1 persistent responses passed on the emulator after the initial HTTP/1.0
close-on-response fixture produced a header EOF. There is no production cleartext-policy change.
Camera/gallery chooser UI is not automated here; their existing callback paths feed
the same tested upload function. Device use/restart was authorized in this session.
