# Read-o-Clock

A real wall clock, alone on the screen, for teaching a young child to read
the time. The hands are the teaching tool: drag either one and the clock
keeps running. Native Android, paid once, fully offline: no ads, no
trackers, no accounts, no network, no permissions. Open source under MIT.

Status: in development. The rules, the app, the icon and the store kit are
in place and green; the first signed release follows once the keystore
exists.

<p align="center">
  <img src="play-store/screenshots/phone/02_one_oclock.png" width="170" alt="One o'clock: the hour hand on 1, the minute on 12">
  <img src="play-store/screenshots/phone/03_half_past_three.png" width="170" alt="Half past three: the hour hand exactly between 3 and 4">
  <img src="play-store/screenshots/phone/01_hero_ten_past_ten.png" width="170" alt="Ten past ten, the open-hands look">
</p>

- **Play Store package:** `io.github.muntasimulhaque.readoclock`
- **License:** MIT
- **Price:** paid once, no ads, no purchases, no subscriptions
- **Privacy policy:** [online](https://muntasimulhaque.github.io/read-o-clock/privacy.html) · [in this repo](docs/privacy.html)

## The clock

One clock on one screen, nothing else. It opens on the real local time and
keeps it, with a second hand that ticks the way a quartz wall clock ticks:
one step per second with a short overshoot and settle. The hour and minute
hands move continuously, and they are geared: move the long hand and the
short hand follows it, move the short hand and the long hand winds around.
The second hand keeps running through every setting.

Drag a hand with a finger. A light magnetic settle makes exact minutes easy
to land on, so one o'clock can be exactly one o'clock. There is no menu, no
settings, no digital readout, and no sound. Closing and reopening the app
returns the clock to the real time.

## Layout

```
core/     its own Gradle module, pure Kotlin, zero Android imports: the
          dial proportions, the gear law, the quartz tick, the spoken time
app/      Compose: the dial, the numerals, the ticks, the hands, the drag
tools/    offline generators: design takes, launcher icon, store art
```

The rules are pure data and functions; Android is a player of those rules,
not a participant. No composable takes a ViewModel.

## Build

`JAVA_HOME` must point at the Android Studio JBR (it is not on PATH):

```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :core:test :app:testReleaseUnitTest :app:lintRelease
./gradlew :app:assembleRelease
```

Design takes (scratch, never committed):

```
./gradlew :tools:makeTakes    # renders build/takes/*.png for review
```

Launcher icon (generated from code, committed, pinned):

```
./gradlew :tools:makeIcons    # regenerate after a deliberate design change
./gradlew :tools:checkIcons   # fails if the committed bytes drift
```
