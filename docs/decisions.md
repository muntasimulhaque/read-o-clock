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
