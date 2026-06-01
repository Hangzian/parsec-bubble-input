# Contributing

Thanks for your interest in Parsec Bubble Input.

This project is intentionally small. The goal is to keep the Android overlay, Mac bridge, and optional relay easy to understand, audit, and run locally.

## Good First Contributions

- Improve Android overlay usability on more screen sizes
- Improve documentation and setup screenshots
- Add safer Mac bridge startup helpers
- Test direct LAN, Tailscale, and relay setups
- Improve accessibility labels and UI copy

## Development Setup

Requirements:

- Android SDK / Android Studio
- Node.js 20+
- Python 3.10+

Useful checks:

```sh
./gradlew :app:assembleDebug
node --check mac/server.js
python3 -m py_compile relay/relay.py
```

## Pull Request Guidelines

- Keep changes focused.
- Do not commit tokens, IP addresses, APKs, logs, or local config files.
- Update `README.md` when setup or behavior changes.
- Prefer simple platform APIs over heavy dependencies.
- Explain the user-facing workflow your change improves.

## Project Scope

In scope:

- Android floating input overlay
- Mac paste bridge
- Optional lightweight relay
- Documentation and setup reliability

Out of scope:

- Replacing Parsec
- Building a full remote desktop client
- Cloud-hosted accounts or managed backend services
- Keyboard/mouse streaming beyond text input
