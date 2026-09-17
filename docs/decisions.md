# Decisions

Why this, not the alternatives. History, not rules: rules live in
AGENTS.md, behavior lives in the code. Add an entry when a choice is made
that a future session could not recover from the code.

## D-001: The name is Read-o-Clock, the package is readoclock

Date: the first session.

The app teaches reading a clock, so the name puns on reading and on
o'clock at once, and it is the only name checked that had zero results in
Google Play, the App Store, and exact-phrase web search. Rejected:
Clocker (four Play apps, a film with a different meaning), Clocket and
Tockle (Play apps surfaced by the checker), Time Teller and Hands of Time
(taken), Clock & Tell (strong, but the owner chose the reading pun).
Package `io.github.muntasimulhaque.readoclock` is permanent, matching the
owner's family of apps.

## D-002: Paid once, fully offline, zero permissions

Date: the first session.

Paid once like Puzzlet and Crayoner. No ads, no trackers, no accounts, no
network, no third-party SDKs, MIT source. The app declares zero manifest
permissions; the Data-safety declaration and the Families listing rest on
it. Data is not collected because there is nothing to collect.

## D-003: One time value, real gears, a real quartz tick

Date: the first session.

The hands are not independent state. One time value drives all three, and
setting a hand solves for the time under the finger; the other hands then
follow the real gear ratios, the second hand included: the minute hand's
position is the minutes plus the seconds, so turning the hands turns the
seconds too. This is what makes the app feel like holding a real clock
instead of a toy: at 1:30 the hour hand sits exactly between 1 and 2,
crossing 12 while dragging rolls the hour, and setting the hands to twelve
lands an exact hour with the second hand on its twelve.

The second hand takes a quartz step once per second with an overshoot and
settle (ease-out-back, about 0.4 degrees of overshoot); the hour and
minute hands move continuously. Rejected: a sweeping second hand (not
this clock), a perfectly still hand until the next tick (a real quartz
hand visibly bounces), independent hands (not a clock), and a second hand
that keeps its phase while the minute hand is turned (physically
impossible on a geared clock).

The hands have a weak magnetic settle of 2.4 degrees, so a released minute
hand eases onto a whole minute, second hand and all, when it is close;
the hour hand always sits proportionally, never parked on a numeral.

## D-004: The clock is the whole app

Date: the first session.

No menus, no settings, no chrome, no digital readout, no sound. Fullscreen
and immersive, the screen stays on while the app is open, portrait and
landscape, phone and tablet. A cold start resets to live local time;
backgrounding does not. Rejected: a Now button, a settings sheet, a
digital companion, ticking sounds (all chrome the teaching moment does
not need).

## D-005: The numeral face is Baloo 2 ExtraBold

Date: the first session.

The family's own OFL font, already used by Puzzlet and Count & Play, and
its round, chunky numerals stay legible at a glance for a young child.
Rejected: system fonts (they differ per device), a serif schoolhouse face
(legible but colder for a 3.5-year-old).

## D-006: The design takes, and which one won

Date: the first session.

Three takes rendered by `:tools:makeTakes` from the same geometry the app
uses: Schoolhouse enamel (cream dial, ink numerals, red second, dark
case), Porcelain kitchen (white dial, steel case), Sunrise (warm ivory
dial, deep blue numerals, coral second, wood case). The owner picks from
`build/takes`; the winner is recorded here and encoded in the app theme.

Chosen: Schoolhouse enamel. Its palette is `core/DialPalette.kt`, the one
source the app theme and the offline generators both read, so the takes,
the app, the launcher icon and the store art can never drift apart.

## D-007: Signing uses the shared upload keystore

Date: the first session.

The app signs with the shared upload keystore in the owner's vault, the
same key the other apps use, rather than spending a day creating and
recording a new one. Play App Signing holds the app signing key; the upload
key only has to stay available to CI, and if it is ever lost it can be
reset with Google. A dedicated keystore would only add one more secret to
keep safe. The build probes a per-app keystore first, so a dedicated key
can take over later without a code change.

## D-008: The submitted listing says clock reading

Date: 17 September 2026, at the 0.1 submission.

The owner changed the store name and short description at submission to
"Read-o-Clock: Clock Reading" (27 characters) and "Teach your child how to
read the clock." (39 characters), choosing the phrase parents actually
type over the friendlier first draft. The launcher name stays Read-o-Clock,
and the submission guide carries the submitted text and its measured
counts. Rejected: "Read-o-Clock: Teach Kids Time" (the first draft).

## D-009: The smaller mark, tap-safe hands, and the tick

Date: 17 September 2026, the day of the 0.1 submission.

Three changes the owner asked for after submitting 0.1. The launcher mark
now sits at 0.8 of its old size (`MarkScale` in `IconMark.kt`), because
the dial had filled the adaptive safe zone edge to edge; the launcher set
and the 512 store icon were regenerated and the icon pin still holds. A
finger must now cross touch slop before a hand moves: the hand is still
picked at the down point, but a tap, however jittery, can no longer jump
the nearest hand across the dial. And the second hand ticks: one soft
synthesized click, written by `:tools:makeTick` to `res/raw/tick.wav` and
played by `TickPlayer` only while the screen is resumed and only when the
running clock advances one whole second.

The tick reverses D-004, which rejected ticking sounds as chrome, and it
overrides hard constraint 4's silence: the owner asked for the little
sound a real wall clock makes, so the rules now allow exactly one. This is
the 0.2 release candidate. Rejected for the tick: a `ToneGenerator` beep
(not a clock), a bundled recording (license and drift), and ticks while
dragging (a buzz the setting moment does not need).

## D-010: A held hand, and a tick measured from real clocks

Date: 17 September 2026, after the 0.2 review.

Two fixes the owner asked for after living with 0.2.

The hand grab: until now any touch on the screen picked the nearest hand,
so a drag that began on the dial, the case or off the clock pulled a hand
around, which is not how a clock works. `HandPick.nearest` now returns
nothing unless the touch lands on a hand (the baton's half width plus a
finger's reach of 0.07 case radii), and the drag begins only then. Tests
pin the empty dial, the corner of the screen and the space beyond a tip.

The tick: 0.2's single synthesized click did not sound like the clock on
the wall. Three public domain recordings of real clocks were measured: the
PDsounds `Clock ticking.ogg` and `Alarm clock ticking.ogg` on Wikimedia
Commons, and the Freesound preview of `Wall Clock Ticks, Quartz Clock` by
Kinoton, used only as a measuring stick and never bundled. Every tick is
two impacts about ten to thirteen milliseconds apart, the second nearly as
loud as the first, and each impact is a noise-rich transient whose
spectrum peaks between 200 Hz and 1 kHz and carries to about 5 kHz, not a
tone with silent gaps between partials. `:tools:makeTick` now models both
impacts, each with its own noise, detuned partial companions and a low
body, at 44.1 kHz. The owner approved the 0.3 release candidate
(versionCode 3) and submitted it to Play the same day.

## D-011: The tick as one knock, and the shadow under the case

Date: 17 September 2026, after the 0.3 review.

Two changes the owner asked for after living with 0.3.

The tick: the owner still did not hear the clock on the wall in 0.3's
sound. Its two impacts sat ten and a half milliseconds apart and nearly
equal in loudness, which reads as a synthetic double click, and its case
modes died in three to six milliseconds, so the tick had no body. The
second impact D-010 measured was probably the room answering the first
hit rather than the movement itself; baked in at full loudness it turns a
knock into a flam. `:tools:makeTick` now lands one knock at full
loudness, a duller catch at half loudness ten and a half milliseconds
later, and a weak rattle at eighteen and a half, over a case bank that
rings from sixteen milliseconds at 300 Hz down to two at 6 kHz, with a
low body under it: fifty two milliseconds in all, ending in a ten
millisecond tail. Rejected: keeping the equal double impact (the 0.3
sound), a click with no case ring (no body), and a recorded sample
(license and drift).

The shadow: the old render drew a ring of black at 38/255 under the case
that all but vanished against the warm field, and the owner said the
clock had no shadow at all. The dial now draws two pools, in the app and
in the takes renderer from the same numbers: a broad halo at 40/255 that
reaches 1.10 case radii, and a contact pool at 34/255 whose dark gathers
under the bottom rim. Nothing reaches past 1.10 case radii, because on a
short window the case nearly touches the screen and a hard clip would
show at the edge; on the 10 inch landscape capture the shade dies before
the screen does. The feature graphic and all eighteen store captures
were regenerated from the same renderers. The owner picks the final
sound from `C:\tmp\ticklab\listen\` before the next release candidate
bumps `versionCode`.
