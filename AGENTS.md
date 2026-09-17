# Read-o-Clock: working rules

A real wall clock, alone on the screen, for teaching a young child to read
the time. The hands are the teaching tool: drag either one and the clock
keeps running, the way a real clock runs while you set it. Native Android,
paid once, fully offline: no ads, no trackers, no accounts, no network, no
permissions. Open source under MIT.

This file is the always-loaded context: the working law, the hard
constraints, the map, and only what the code cannot say. Mechanics,
numbers, colors and copy live in the code and its tests. If a rule here
and the code disagree, the code is what ships and this file gets fixed.

## This file is not the final word

The rules are the current best understanding, not the ceiling. If a
change you believe in contradicts one, do not drop it silently and do
not implement against the rule either: name the conflict, make the case,
and let the owner decide. An overridden rule is updated here, never left
as a dead letter.

## Every session

1. Read this file.
2. `git fetch` and pull `main` before any other command: the owner works
   from more than one machine, and no session builds on a stale head.
3. Implement, then ask one question: anything else? The build waits for
   the owner's word; no `versionCode` moves until the session is done.

## Hard constraints (non-negotiable)

1. **The realism law.** One time value drives all three hands; the hands
   are views of it, never independent state. Setting a hand solves for
   the time under the finger and the others follow through the real gear
   ratios: dragging the minute hand carries the hour, dragging the hour
   hand spins the minute, and crossing 12 rolls the hour. The second hand
   keeps its own phase through any setting. Hour and minute hands move
   continuously; the second hand takes a quartz step once per second with
   a visible overshoot and settle, because the hand has inertia. Tests in
   `core/` pin every one of these.
2. **No network, no ads, no trackers, zero permissions.** No `INTERNET`,
   no third-party SDKs, no analytics. This underpins the Data-safety
   declaration and the Families listing. A new permission needs the
   owner's sign-off written here first.
3. **The clock is the whole app.** No menus, no settings, no buttons,
   no digital readout, no chrome. It resets to live local time on a cold
   start and does nothing else. While the app is open the screen stays
   on, fullscreen and immersive, portrait and landscape, phone and tablet.
4. **Silent.** No sound of any kind in v1.
5. **No depiction of animate beings.** No humans, animals, faces,
   mascots, or eyes on objects, in the app, the launcher icon, or the
   store art. The clock itself is the subject; warmth comes from
   material, light, and motion.
6. **The app must not crash.** Defensive code, no `!!`, no unchecked
   casts, no swallowed exceptions, state that survives rotation, tests
   around every rule. A Play-vitals crash is a stop-the-line event.
7. **Accessibility is a rule, not a feature.** TalkBack speaks the time
   in the words a parent would say, and can move the hands. The dial is
   one named target; the hands are not separate traps.

## Style

- No em-dashes, ever, unless truly necessary: not in chat, release
  notes, commit messages, code comments, or this file. Use commas,
  colons, parentheses, or a sentence break.
- American English, everywhere: color, gray, center, license.
- Store text is plain prose; Play Console mangles quotes, markdown and
  dashes. Release notes fit the 500 character field, counted before
  handing them over.
- Files stay under 400 lines, functions under 40. Split early.

## Forbidden

1. `core/` imports nothing from `android.*`; it is plain JVM Kotlin.
2. No composable takes a ViewModel. Screens take state and callbacks;
   the activity wires the host in, which is what lets the screenshot
   harness host every state with no-op callbacks.
3. No user-facing string outside `app/src/main/res/values/strings.xml`.
4. No color literal in UI code; the palette lives in the app theme and
   the take palettes live in `tools/`.
5. No `!!`, no unchecked casts, no swallowed exceptions.
6. No network code, no WebView, no third-party SDKs. AndroidX and Kotlin
   only; each new dependency is proposed here first.
7. No `Co-Authored-By` trailer or any AI attribution in a commit
   message. One person writes this app and the history should say so.

## Shipping

- `applicationId` is permanently `io.github.muntasimulhaque.readoclock`.
  Play ties an app to its first package ID forever; the code namespace
  mirrors it.
- The version walk: `versionCode` only ever increases and is never
  reused; `versionName` is `versionCode` divided by ten, one decimal.
  The live numbers are in `app/build.gradle.kts`.
- `targetSdk` moves only together with an AGP that supports it.
- The signing keystore lives outside the repo (the owner's vault), with
  its base64 twin in the `KEYSTORE_BASE64` GitHub secret and the
  passwords in three more secrets. If it is lost, the app can never be
  updated again. Local builds without it stay unsigned.
- Paid once on Play Console. No billing SDK in the app, ever.

## Build

```sh
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # not on PATH
./gradlew :core:test                                             # the rules
./gradlew :tools:makeTakes                                       # design takes into build/takes
```

`app/src/androidTest` holds the screenshot harness; captures are
reviewed from CI, never pre-checked on a local emulator. `.github/`
workflows are the CI truth.

## Map

```
core/                      pure Kotlin, zero Android imports:
  ClockFace.kt             every dial proportion, one source of truth
  ClockTime.kt             the laws: angles, the gear law, the quartz tick
  SpokenTime.kt            the words TalkBack says
app/src/main/.../host/     ClockHost: the running time, the drag, the frame clock
app/src/main/.../ui/       Compose: the dial, numerals, ticks, hands, the drag target
app/src/main/res/values/   strings.xml: every user-facing string
app/src/main/res/font/     Baloo 2 (OFL), the numeral face
tools/                     offline generators: design takes, launcher icon, store art
docs/                      privacy.html, OFL-Baloo2.txt, decisions.md
play-store/                listing kit, screenshots per form factor, aab/
```

Where truth lives, by question:

- behavior and rules: `core/` and its tests.
- a dial proportion: `core/ClockFace.kt`.
- a word: `app/src/main/res/values/strings.xml` and `core/SpokenTime.kt`.
- a decision, or its history: `docs/decisions.md`.

## Glossary

- **live time**: the real local time, where a cold start begins.
- **taught time**: the time the owner set by hand; from then on it runs.
- **the gear law**: hands are geared the way a clock's are; one time
  value, three views.
- **the take**: one rendered design direction, in `build/takes`.
- **the tick**: one quartz step, including its overshoot and settle.
- **the dial, the case, the ring**: the face, its rim, and the ring of
  sixty ticks.
