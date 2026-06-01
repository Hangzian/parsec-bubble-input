# Parsec Community Outreach

This document contains copy-ready text for sharing Parsec Bubble Input with Parsec users, community spaces, or support channels.

Keep the positioning clear:

- say "unofficial companion tool"
- do not say "official plugin"
- do not use Parsec brand assets
- do not imply endorsement by Parsec or Unity

## Short Community Post

```text
I built an unofficial Android floating input companion for people using Parsec from a phone to control a Mac.

Parsec handles the screen and remote control really well, but long-form text input from Android can still be awkward, especially for Chinese input, voice input, prompts, clipboard snippets, and chat replies.

This tool adds a small Android floating bubble. You type or paste text on the phone, tap send, and a lightweight Mac bridge pastes it into the currently focused Mac app.

Repo:
https://github.com/Hangzian/parsec-bubble-input

It is open source, local-first, token-protected, and does not use any private Parsec APIs. It is just a companion workflow for Parsec users who want better mobile text entry.
```

## Longer Community Post

```text
I use Parsec on Android to control my Mac remotely, and the screen/control side works well enough that I started using it as a real mobile workstation setup.

The part that kept bothering me was text input. Short words are fine, but Chinese input, prompts, URLs, clipboard snippets, and longer replies are painful through a tiny remote desktop field.

So I built Parsec Bubble Input:

- Android floating bubble over Parsec
- compact text input panel
- send current text to Mac
- send clipboard text
- resend last text
- optional auto-send
- lightweight Node.js Mac bridge
- optional Python relay for off-LAN setups
- token-based access control

The idea is simple: Parsec gives you the screen; this tool gives you comfortable text entry.

It is unofficial, open source, and does not integrate with private Parsec APIs. The Android app sends text to your own Mac bridge, and the Mac bridge pastes into the focused window.

GitHub:
https://github.com/Hangzian/parsec-bubble-input

I would love feedback from other Parsec mobile users, especially people using Android phones, Chinese/IME input, voice dictation, or Parsec for productivity rather than only games.
```

## Support / Feedback Note

```text
Hi Parsec team,

I built an unofficial open-source companion tool that improves Android-to-Mac text input for Parsec users:

https://github.com/Hangzian/parsec-bubble-input

The problem it addresses is narrow but recurring: when using Parsec from an Android phone to control a Mac, screen streaming and basic control work well, but long-form text input, Chinese/IME input, voice input, and clipboard snippets can be awkward.

The project keeps Parsec as the screen/control layer and adds:

- Android floating input bubble
- local Mac paste bridge
- token-protected HTTP transport
- optional relay for off-LAN setups

It does not use private Parsec APIs and is not presented as an official plugin. I am sharing it as a reference workflow and would appreciate feedback on whether this kind of mobile text-entry companion is useful to Parsec users.
```

## Suggested Tags

```text
parsec
remote-desktop
android
macos
productivity
ime
clipboard
```
