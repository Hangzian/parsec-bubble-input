# Release Checklist

Use this checklist before publishing a GitHub release.

## Local Checks

- Build Android debug APK:

  ```sh
  ./gradlew :app:assembleDebug
  ```

- Check the Mac bridge:

  ```sh
  node --check mac/server.js
  ```

- Check the optional relay:

  ```sh
  python3 -m py_compile relay/relay.py
  ```

## Manual Smoke Test

- Start the Mac bridge with `mac/start-bridge.sh`.
- Confirm `GET /api/health` returns the bridge name.
- Install the APK on an Android phone.
- Configure bridge URL and token in the app.
- Grant overlay permission.
- Open Parsec and focus a text field on the Mac.
- Send normal text, clipboard text, and resend-last text.
- Confirm collapse, close, and bubble position persistence.

## Public Repo Hygiene

- Confirm no tokens, IP addresses, APKs, logs, or local config files are staged.
- Confirm `README.md`, `LICENSE`, `SECURITY.md`, and `CONTRIBUTING.md` are current.
- Add screenshots or a demo video only after checking they do not reveal private URLs, tokens, or personal content.

## Optional GitHub Actions

If your GitHub token has the `workflow` scope, add a CI workflow that runs the same three checks on pull requests:

- `./gradlew :app:assembleDebug`
- `node --check mac/server.js`
- `python3 -m py_compile relay/relay.py`
