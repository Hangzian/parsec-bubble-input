# OpenAI OSS Support Fit

Parsec Bubble Input is a practical open-source companion tool for mobile-to-desktop workflows.

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
- release checklist and repeatable local checks

## Potential Future Work

- Signed releases
- Better Android onboarding
- More reliable Mac startup integration
- Screenshots and demo video
- Tailscale-specific setup guide
- Local network discovery / QR pairing

The project is intentionally scoped: it focuses on text input for phone-based remote desktop use, not on becoming a full remote desktop system.
