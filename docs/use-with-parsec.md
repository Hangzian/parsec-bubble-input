# Use with Parsec

Parsec Bubble Input is an unofficial companion tool for people who use Parsec on Android to control a Mac.

Parsec handles the low-latency screen stream and remote control. This project focuses on one narrow gap: comfortable long-form text input from a phone.

## Why This Exists

Typing through a phone-based remote desktop session can be awkward:

- remote text fields are small
- cursor placement can be imprecise
- Chinese input and IME composition can be clumsy inside a remote session
- phone voice input is useful, but the recognized text still needs to reach the Mac
- clipboard snippets and prompts are easier to prepare on the phone than to type through the remote screen

Parsec Bubble Input keeps Parsec as the screen/control layer and adds a small Android overlay for text entry.

## How It Fits

```text
Android phone
  Parsec app for screen/control
  Parsec Bubble Input for text

Mac
  Parsec host/session
  Mac bridge for paste delivery
```

When you tap send, the Android overlay sends text to the Mac bridge. The Mac bridge copies that text into the Mac pasteboard and presses `Cmd+V` in the currently focused app.

## Recommended Setup

1. Start the Mac bridge:

   ```sh
   cd mac
   ./start-bridge.sh
   ```

2. Copy the printed URL and token into the Android app.
3. Grant Android overlay permission.
4. Start the floating bubble.
5. Open Parsec on Android and connect to your Mac.
6. Focus a text field on the Mac.
7. Tap the floating bubble, type or paste text, then send.

## Network Modes

### Same LAN

Use the Mac bridge URL printed by `mac/start-bridge.sh`, usually something like:

```text
http://192.168.1.10:8765
```

This is the simplest setup.

### Tailscale or Similar VPN

Use the Mac's private VPN IP or MagicDNS name as the bridge URL. This keeps the bridge private while still allowing off-LAN access.

### VPS Relay

Use the optional relay only when the Android phone cannot directly reach the Mac. In this mode:

```text
Android app -> relay -> Mac bridge -> focused Mac app
```

Use the same token on the Android app, relay, and Mac bridge.

## Security Boundaries

- This is not a Parsec plugin and does not use private Parsec APIs.
- Anyone with the bridge URL and token can send text to the focused Mac window.
- Use a long random token.
- Prefer LAN or private VPN access where possible.
- If a relay is exposed publicly, put it behind HTTPS and firewall rules.
- Only run the Mac bridge on a Mac you control.

## Branding Note

This project is not affiliated with Parsec or Unity. Parsec is referenced only to describe the workflow this companion tool is designed for.
