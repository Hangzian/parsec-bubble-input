# Security Policy

Parsec Bubble Input is a local-first utility that sends text from Android to a Mac bridge. The Mac bridge pastes into the currently focused Mac window, so security and trust boundaries matter.

## Supported Versions

Security fixes target the latest `main` branch until formal releases are created.

## Reporting a Vulnerability

Please open a private security advisory on GitHub if available, or contact the maintainer through the repository owner profile. Avoid posting working exploit details in a public issue before there is a fix.

## Security Model

- The Android app sends text to a bridge URL configured by the user.
- The Mac bridge requires `x-parsec-bubble-token`.
- The optional relay also requires the same token.
- The Mac bridge copies text into the pasteboard and simulates `Cmd+V`.

## Recommendations

- Use a long random token.
- Prefer LAN or Tailscale for direct use.
- If exposing a relay publicly, put it behind HTTPS and firewall rules.
- Do not commit `bridge.env`, `.token`, relay env files, logs, or build artifacts.
- Run the Mac bridge only on a Mac you control.

## Known Risks

- Anyone with the bridge URL and token can send text to the focused Mac window.
- macOS Accessibility permission is required for paste automation.
- A public HTTP relay without TLS can leak traffic metadata and content.
