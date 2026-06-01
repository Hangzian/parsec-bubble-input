# OpenAI OSS Support Fit

Parsec Bubble Input is a practical open-source companion tool for mobile-to-desktop workflows.

Official OpenAI application pages to review before submission:

- Codex open source fund: `https://openai.com/form/codex-open-source-fund/`
- Codex for Open Source: `https://openai.com/form/codex-for-oss/`

## Summary

The project helps Android users remotely control a Mac through Parsec while solving a specific usability gap: text input. A phone is good at voice input, clipboard handling, and native keyboard entry; a Mac is good at desktop apps. This project bridges those strengths with a small Android overlay and a lightweight local Mac bridge.

## Real User Problem

Remote desktop from a phone often works visually but fails ergonomically when the user needs to type:

- remote text fields are small
- cursor placement is imprecise
- Chinese and long-form text input can be painful
- mobile voice input is not naturally connected to the remote Mac app

Parsec Bubble Input lets users enter text naturally on the phone and paste it into the focused Mac app.

## Why It Is Useful

- It improves an existing remote desktop workflow without replacing Parsec.
- It is local-first and self-hostable.
- It uses simple protocols and minimal dependencies.
- It is understandable enough for other developers to audit and extend.
- It can support accessibility and mobile productivity workflows.

## Project Completeness

The repository includes:

- Android app source
- Mac bridge source
- optional relay source
- token-based access control
- setup scripts
- README and FAQ
- MIT license
- contribution and security docs
- GitHub Actions for repeatable validation
- release checklist and repeatable local checks

## Potential Future Work

- Signed releases
- Better Android onboarding
- More reliable Mac startup integration
- Screenshots and demo video
- Tailscale-specific setup guide
- Local network discovery / QR pairing

The project is intentionally scoped: it focuses on text input for phone-based remote desktop use, not on becoming a full remote desktop system.

## Application Draft

Use this section as copy-ready source material when filling out an OpenAI OSS support form.

### Which open source project are you representing?

Parsec Bubble Input

### GitHub repository

https://github.com/Hangzian/parsec-bubble-input

### Brief description of the project

Parsec Bubble Input is an Android floating bubble input companion for people who control a Mac remotely through Parsec. Remote desktop from a phone works well for seeing and clicking, but text input is often painful, especially for Chinese input, voice dictation, clipboard snippets, prompts, and longer messages. The project provides an Android overlay for typing or pasting text, a lightweight Mac bridge that receives text and pastes it into the currently focused Mac window, and an optional relay for off-LAN setups.

The project is intentionally small and self-hostable: it uses a native Android overlay, a Node.js Mac bridge, a simple token-protected HTTP API, and an optional Python relay. It includes setup documentation, security notes, release packaging, GitHub Actions validation, and an MIT license.

### Why this project has real OSS value

This solves a real workflow gap for mobile remote desktop users. It improves an existing toolchain instead of trying to replace it: Parsec handles screen streaming, while Parsec Bubble Input handles text entry. The code is small enough to audit, easy to clone and run, and useful for developers, creators, accessibility workflows, and anyone who uses a phone as a remote workstation terminal.

### How API credits or Codex support would be used

Support would be used to improve the project as a maintainable open-source tool:

- use Codex to review pull requests and catch Android overlay regressions
- automate release checklist validation for Android, Mac bridge, and relay changes
- generate and maintain setup documentation for common network modes such as LAN, Tailscale, and VPS relay
- improve onboarding copy, FAQ coverage, and troubleshooting flows
- explore optional AI-assisted diagnostics for configuration issues while keeping the core app local-first and token-protected

### Current project status

The project is public, MIT licensed, and includes a working Android app, Mac bridge, optional relay, GitHub Release APK, documentation, security policy, contribution guide, and passing CI.
