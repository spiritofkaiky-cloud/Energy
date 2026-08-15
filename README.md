# ⚡ Energy

A beautiful Android app that tracks your health like Apple Fitness and records your
outdoor workouts with a live GPS map route like Strava.

**Stack:** Kotlin · Jetpack Compose · Material 3 · Health Connect · Google Maps SDK · Supabase

The full build spec lives in [`../APP_SPEC.md`](../APP_SPEC.md).

## Status

| Milestone | What | Status |
|-----------|------|--------|
| M0 | Project scaffold, Gradle, wrapper | ✅ |
| M1 | Design system, splash, sign-in (Google + guest), tab navigation | ✅ |
| M2 | Health Connect dashboard (steps / HR) with graceful degradation | ✅ |
| M3 | Live GPS tracking + full-screen map + foreground service | ✅ |
| M4 | Workout history + route detail + full-screen day map | ✅ |
| M5 | Supabase cloud sync + Google sign-in (code ready — see ☁️ Cloud setup) | 🟡 needs your Supabase project |
| M6 | Goals, achievements, streaks | ✅ streaks + achievements + workout alarm |
| M7 | Polish: theme switcher, haptics | ✅ Oura-style polish (v0.3.1) |
| M8 | Tests + Play Store release | ⏳ |

### v0.3.1 — Oura-smooth design pass
- 💯 **Energy Score** — Oura-style daily 0-100 gauge (steps + workout distance + day path), glowing arc with hero numeral
- ✨ **Breathing glow rings**, hairline-border cards, huge light-weight numerals, springy motion, press feedback
- 🔥 **Activity streaks + achievement badges** (3/7/14/30 days)

### v0.3 features
- 🏃 **Live workouts** — pick Run/Walk/Cycle/Hike, full-screen real map with the route drawing live, timer, pause/resume, finish summary (distance/time/pace/kcal), foreground-service notification
- 🗺️ **Real map everywhere** — pan/zoom/rotate/compass, dark map in dark mode (MapLibre + OpenFreeMap, free, no API key)
- 📚 **History** — saved workouts with route thumbnails → full route detail screen
- ☁️ **Cloud-ready** — Supabase REST sync + Google sign-in via CredentialManager (see below)
- ❤️ **Health Connect** — steps + heart rate on Home when available
- 🌗 Theme switcher (System/Light/Dark) + ⏰ workout alarm (v0.2)

## ☁️ Cloud setup (M5 — 10 minutes, free)

1. **Supabase project** — create a free project at https://supabase.com → Settings → API → copy the **Project URL** and **anon public key**
2. **Table** — in Supabase SQL Editor, run:
   ```sql
   create table public.workouts (
     id uuid primary key default gen_random_uuid(),
     user_id uuid references auth.users default auth.uid(),
     payload jsonb not null,
     created_at timestamptz default now()
   );
   alter table public.workouts enable row level security;
   create policy "own workouts" on public.workouts
     for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
   ```
3. **Google sign-in** — Google Cloud Console → APIs & Services → Credentials → **Create OAuth client ID** → type **Web application** → copy the **Client ID**
4. **Wire it up** — edit `local.properties` (never committed):
   ```properties
   supabase.url=https://YOURPROJECT.supabase.co
   supabase.key=YOUR_ANON_KEY
   google.clientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
   ```
5. Rebuild: `gradlew.bat assembleDebug` — Google sign-in and workout cloud sync activate automatically. Without these keys the app runs perfectly local-only.

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
