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
4. Session end: append the session's decision entry to
   `docs/decisions.md`, bring the README status and the submission guide
   current, commit and push, and leave the tree clean.

## Hard constraints (non-negotiable)

1. **The realism law.** One time value drives all three hands; the hands
   are views of it, never independent state. Setting a hand solves for
   the time under the finger and the others follow through the real gear
   ratios, the second hand included, because the minute hand's position
   is the minutes plus the seconds. Dragging the minute hand carries the
   hour, dragging the hour hand spins the minute, and crossing 12 rolls
   the hour. Hour and minute hands move continuously; the second hand
   takes a quartz step once per second with a visible overshoot and
   settle, because the hand has inertia. A released hand settles onto a
   whole minute when it is close, so one o'clock can be exactly one
   o'clock with the second hand on its twelve. Once released, the whole
   clock runs. Tests in `core/` pin every one of these.
2. **No network, no ads, no trackers, zero permissions.** No `INTERNET`,
   no third-party SDKs, no analytics. This underpins the Data-safety
   declaration and the Families listing. A new permission needs the
   owner's sign-off written here first.
3. **The clock is the whole app.** No menus, no settings, no buttons,
   no digital readout, no chrome. It resets to live local time on a cold
   start and does nothing else. While the app is open the screen stays
   on, fullscreen and immersive, portrait and landscape, phone and tablet.
4. **One small sound, the tick.** The second hand clicks softly once
   per second, the way a quartz wall clock does, and that tick is the
   only sound: no music, no voices, no effects, no in-app volume
   control. It plays only while the screen is resumed and never while a
   finger is dragging.
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
  The first release is 0.1, then 0.2 through 0.9, then 1.0, 1.1 and on.
  The live numbers are in `app/build.gradle.kts`.
- `targetSdk` moves only together with an AGP that supports it.
- Signing uses the shared upload keystore in the owner's vault (D-007),
  never in the repo. Its base64 twin and the passwords live in this repo's
  `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`
  secrets; a per-app keystore would win if one is ever created. Play App
  Signing holds the app signing key, so a lost upload key can be reset
  with Google, and the vault is backed up in a second place anyway.
- Paid once on Play Console. No billing SDK in the app, ever.

## Build

```sh
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # not on PATH
./gradlew :core:test                                     # the rules
./gradlew :app:testReleaseUnitTest :app:lintRelease      # app tests and full lint
./gradlew :app:assembleDebug                             # installable debug build
./gradlew :app:assembleRelease                           # R8 release, signed when the keystore is present
./gradlew :app:bundleRelease                             # the AAB that goes to Play
./gradlew :tools:makeTakes                               # design takes into build/takes
./gradlew :tools:makeArt                                 # feature graphic and 512 store icon
./gradlew :tools:makeIcons :tools:checkIcons             # regenerate, then pin the launcher icon
./gradlew :tools:makeTick :tools:checkTick               # regenerate, then pin the quartz tick
```

## Verifying UI: CI is the loop

UI changes are verified from CI screenshot artifacts, never by reading
code and never from a local emulator; a local headless capture once came
back black and proved nothing. CI emulators render and capture reliably.

- `build.yml` (every push touching app, core or tools, plus PRs and manual
dispatch): rules tests, release lint, the icon and tick pins, the debug
APK, and a minified release AAB and APK. With the signing secrets present
it also verifies the certificate and publishes both to the `latest-build`
release.
- `screenshots.yml` (pushes touching the app or core): six scenes per form
factor on API 35 emulators, phone, 7 inch and 10 inch, artifacts named
`store-screenshots-*`.
- The harness is `app/src/androidTest/.../ScreenshotTest.kt`; its bare host
activity is declared debug-only in `app/src/debug/AndroidManifest.xml`.
- A no-change refresh must come back byte-identical: compare captured PNGs
with `cmp` against `play-store/screenshots/`. Byte noise is not drift.
- `gh run download` has no `--clobber`; a failed re-download leaves the old
files in place and looks like unchanged captures.

## Releasing

Small commits, plain messages, no AI trailers. A release candidate bumps
`versionCode` +1 and lets `versionName` follow the walk, pushes to `main`,
and CI does the rest, ending at the `latest-build` release:

```sh
gh release download latest-build -R muntasimulhaque/read-o-clock -p "*.aab" -D play-store/aab
```

The AAB sits in `play-store/aab/` until the owner confirms the Play
submission; then DELETE it, so a stale build can never be uploaded twice.
Release notes arrive in chat as bare plain text, under 500 characters,
counted before handing them over, and are stored in the submission guide.
The listing kit and the Console answers live in
`play-store/play-store-submission-guide.md`. The privacy policy is served
from `docs/privacy.html` by GitHub Pages.

## Traps that already bit

- On a geared clock the minute hand's position is the minutes plus the
  seconds, so the second hand cannot keep its phase while the minute hand
  is turned. The first drag model assumed it could; the tests caught it,
  and D-003 records the physical truth.
- Appending an oval to a polygon path can cancel it under the non-zero
  winding rule and leave a white notch at the tip. Fill the tip circle
  separately; the icon mark and the Compose hands both do.
- A long-lived effect (the frame loop) holds the callbacks it was born
  with. Read them through `rememberUpdatedState`, and key each screenshot
  scene, or every capture renders the first scene: all eighteen captures
  once came out at 10:10.

## Map

```
core/                      pure Kotlin, zero Android imports:
  ClockFace.kt             every dial proportion, one source of truth
  ClockTime.kt             the laws: angles, the gear law, the quartz tick
  ClockSetter.kt           the one mutable thing: the offset from live time
  HandPick.kt              which hand a finger grabbed
  ClockLayout.kt           how large the clock is on a window
  DialPalette.kt           the chosen Schoolhouse palette (D-006)
  SpokenTime.kt            the words TalkBack says
app/src/main/.../host/     ClockHost: the wall clock and the offset;
                           TickPlayer: the audible quartz tick
app/src/main/.../ui/       Compose: ClockScreen (frame loop, drag, TalkBack),
                           ClockDial, Numerals, Hands, ClockTheme
app/src/androidTest/       ScreenshotTest.kt: the six store captures
app/src/debug/             the debug-only bare host activity for the harness
app/src/main/res/values/   strings.xml: every user-facing string
app/src/main/res/font/     Baloo 2 (OFL), the numeral face
app/src/main/res/raw/      tick.wav: the synthesized second-hand click
tools/                     offline generators: takes, launcher icon, store
                           art, the tick
docs/                      privacy.html, OFL-Baloo2.txt, decisions.md
play-store/                listing kit, screenshots per form factor; aab/
                           holds only the build awaiting submission
.github/workflows/         build.yml, screenshots.yml
```

Where truth lives, by question:

- behavior and rules: `core/` and its tests.
- a dial proportion: `core/ClockFace.kt`.
- a word: `app/src/main/res/values/strings.xml` and `core/SpokenTime.kt`.
- a sound: `app/src/main/res/raw/tick.wav` and its `:tools:makeTick`
  generator.
- a decision, or its history: `docs/decisions.md`.

## Glossary

- **live time**: the real local time, where a cold start begins.
- **taught time**: the time the owner set by hand; from then on it runs.
- **the gear law**: hands are geared the way a clock's are; one time
  value, three views.
- **the take**: one rendered design direction, in `build/takes`.
- **the tick**: one quartz step, including its overshoot, its settle,
  and the soft click that goes with it.
- **the dial, the case, the ring**: the face, its rim, and the ring of
  sixty ticks.
