# ⚡ Energy

A beautiful Android app that tracks your health like Apple Fitness and records your
outdoor workouts with a live GPS map route like Strava.

**Stack:** Kotlin · Jetpack Compose · Material 3 · Health Connect · Google Maps SDK · Supabase

The full build spec lives in [`../APP_SPEC.md`](../APP_SPEC.md).

## Status

| Milestone | What | Status |
|-----------|------|--------|
| M0 | Project scaffold, Gradle, wrapper | ✅ |
| M1 | Design system, splash, Google sign-in UI + guest mode, tab navigation | ✅ |
| M2 | Health Connect dashboard (steps / HR / rings) | ⏳ next |
| M3 | Live GPS tracking + animated map path | ⏳ |
| M4 | Workout summary + history | ⏳ |
| M5 | Supabase sync + real Google sign-in | ⏳ |
| M6 | Goals, achievements, streaks, notifications | 🟡 exercise alarm done |
| M7 | Polish: dark mode audit, haptics, empty states | 🟡 theme switcher done |
| M8 | Tests + Play Store release | ⏳ |

### v0.2 extras (requested features)
- 🌗 **Theme switcher** — System / Light / Dark, persisted (Profile → Appearance)
- ⏰ **Exercise reminder** — daily alarm via `setAlarmClock`, notification on fire
- 🗺️ **Today's Movement map** — Strava-style day-path line on Home (MapLibre + OpenFreeMap, free, no API key, dark map style in dark mode)
- 📍 **Passive day tracking** — fused provider with automatic framework-GPS fallback (heals broken GMS state, works on GMS-free devices)

## Open in Android Studio

1. Android Studio → **Open** → select this folder (`fitness-app/Energy`)
2. Wait for Gradle sync (first run downloads dependencies)
3. Run ▶ on an emulator or your phone

## Build from the terminal

```bash
# Windows (from this folder)
gradlew.bat assembleDebug        # builds debug APK
gradlew.bat installDebug         # installs on a connected device/emulator
```

If Gradle can't find a JDK, use the Temurin 21 install for this project (the
Android Studio bundled JBR is JDK 25, which Kotlin 2.1.x cannot parse):

```bash
export JAVA_HOME="C:\Users\Adidi\jdk-21"
```

> `local.properties` with `sdk.dir` is created automatically by Android Studio on first open.

## Project structure

```
app/src/main/java/com/energy/app/
├─ di/            # AppContainer (manual DI until Hilt lands in M5)
├─ data/auth/     # AuthRepository seam — guest now, Supabase later
└─ ui/
   ├─ navigation/ # EnergyNavHost + MainScaffold (4 tabs)
   ├─ theme/      # Energy design system (colors, type, shapes, motion)
   ├─ components/ # EnergyButton, ActivityRing, SkeletonBox, GradientBackground
   └─ screens/    # splash, auth, home, workout, history, profile
```
