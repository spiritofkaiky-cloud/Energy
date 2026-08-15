# ⚡ Energy

A premium Android fitness app: health tracking like Apple Fitness, live GPS
routes like Strava, and an Oura-style daily Energy Score — with its own identity.

**Stack:** Kotlin · Jetpack Compose · Material 3 · Health Connect · MapLibre + OpenFreeMap · Supabase REST

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
| M6 | Goals, achievements, streaks, personal records | ✅ PRs + streaks + achievements + goals |
| M7 | Polish: theme switcher, haptics, motion design | ✅ |
| M8 | Tests + Play Store release | 🟡 55 unit tests green; Play Store pending |

### v0.5.1 — 2026 premium redesign
- 🎨 **New design language** — semantic color system (layered blue-black dark /
  warm-paper light), typographic confidence (light-weight display numerals with
  tabular figures, uppercase metadata labels), spacing/shape/motion tokens
  (`Space`, `Radius`, `Motion`, `MetaLabel`), orange demoted to a true accent.
- 🏠 **Home rebuilt as a daily briefing** — context line → greeting → Energy
  Score hero (arc + glow + explainable factors) → daily insight → one integrated
  **Energy Ring** with legend → compact stat strip → edge-to-edge movement map
  with floating metrics → quiet streak/health footer. Sections, not cards.
- 🧭 **4-tab navigation** — TODAY / ACTIVITY / PROGRESS / PROFILE with a custom
  hairline bar, accent state, spring lift and haptic ticks. History merged into
  the ACTIVITY tab as a chronological timeline (filters preserved).
- 🏃 **Workout experience rebuilt** — "Ready to move?" entry with a visual
  type selector and goal chips; the live screen is a sports instrument (giant
  current-pace hero, distance/time/speed row, big physical controls); PAUSED
  morphs the whole screen; the summary celebrates first (hero numbers + PRs +
  insights) and analysis lives in the detail screen's Performance/Effort/
  Highlights sections.
- 📈 **Progress analytics** — WEEK / MONTH / YEAR with one major chart at a
  time (smooth gradient line chart / rounded bars), headline + delta, then
  consistency, records and all-time numbers.
- 👤 **Profile as a control center** — identity + lifetime stats, grouped
  GOALS / WORKOUT / APPEARANCE / REMINDER / CLOUD sections with steppers and
  toggles instead of form rows.
- 🧹 Dead components removed; charts, rings, buttons and empty states are now
  shared design-system components (no per-screen styling).

### v0.5 — Reliability + product transformation
- 🛡️ **Crash-safe workouts** — every GPS fix is journaled to disk instantly; a killed
  process loses at most the last fix, and the session is restored as a paused draft
  on next launch (resume / finish / discard). Finish only claims "saved" once the
  workout is really on disk; failed saves keep the draft instead of losing data.
- 🧭 **GPS quality gate** — accuracy ceiling, impossible-jump rejection, spike
  rejection, duplicate minimization (unit-tested). Distance comes only from
  accepted fixes.
- 💾 **New storage** — small versioned metadata + per-workout route files with
  atomic writes, backup restore, and automatic migration from the v0.4 blob.
  One corrupt workout can never wipe your history.
- 🏠 **Home rebuilt** — Energy Score with category/trend/explainable factors,
  daily recommendation (rule-based, transparent), real activity rings
  (move/exercise/stand), today's stats, workout-in-progress banner.
- 📈 **Progress tab** — 14-day distance chart, Energy Score trend, consistency,
  personal records, lifetime totals (pure Canvas charts, no chart library).
- 🏆 **Personal records + insights** — fastest 1 km / 1 mile / 5 km, longest
  distance/time, best day; per-workout insights from your own history.
- 🏃 **Workout flow upgraded** — 3-2-1 countdown, haptics, GPS-readiness card,
  accidental-touch protection on Finish, speed-colored route, splits, elevation.
- 📚 **History** — date-grouped timeline, type filters, sync badges, delete.
- 👤 **Profile** — real lifetime stats, weight + step goal settings, cloud-sync
  status with retry, persisted session (relaunch skips sign-in).
- 🌙 Health Connect now uses your local timezone for daily totals.

### v0.4.1 — Theme fixes
- 🌗 Dark mode works (night XML theme + painted background).
- 🖥️ Theme switcher on the sign-in page.

### v0.4 — Richness pass
- 👤 Email + Google accounts (Supabase), guest mode, settings (units, battery saver,
  auto-pause, calorie goal, alarm), Help & Contact, aurora background, speed tracker.

### v0.3.1 — Oura-smooth design pass
- 💯 Energy Score gauge, breathing glow rings, hairline cards, streaks + badges.

### v0.3
- 🏃 Live workouts, real map everywhere (MapLibre + OpenFreeMap, free, no API key),
  history with route thumbnails, Health Connect steps/HR.

## ☁️ Cloud setup (10 minutes, free)

1. **Supabase project** — https://supabase.com → Settings → API → copy **Project URL** + **anon key**
2. **Table** — SQL Editor:
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
3. **Google sign-in** — Google Cloud Console → Credentials → OAuth client ID (Web application)
4. **Wire it up** — `local.properties` (never committed):
   ```properties
   supabase.url=https://YOURPROJECT.supabase.co
   supabase.key=YOUR_ANON_KEY
   google.clientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
   ```
5. Rebuild — Google sign-in and workout sync activate automatically.
   Without keys the app runs perfectly local-only (offline-first by design).

## Build from the terminal

```bash
export JAVA_HOME="C:\Users\Adidi\jdk-21"   # Temurin 21 (AS JBR = JDK 25 breaks Kotlin 2.1.x)
gradlew.bat clean
gradlew.bat test          # 56 unit tests: GPS filter, PRs, score engine, codec, math
gradlew.bat assembleDebug
```

## Project structure

```
app/src/main/java/com/energy/app/
├─ di/            # AppContainer (manual DI) + application scope
├─ data/
│  ├─ auth/       # PersistedAuthRepository (session survives restart)
│  ├─ cloud/      # Supabase REST (sign-in, workout sync, sync states)
│  ├─ health/     # Health Connect (steps/HR, local-timezone daily bounds)
│  ├─ location/   # LocationTracker (filtered, batched, battery-aware) + DayPath
│  ├─ settings/   # DataStore prefs (units, goals, weight, alarm, theme)
│  ├─ stats/      # EnergyScoreEngine (pure, explainable) + lifetime stats
│  └─ workout/    # WorkoutSession (crash-safe), GpsFilter, WorkoutMath,
│                 # WorkoutRepository (split storage + migration), PRs, insights
└─ ui/
   ├─ navigation/ # NavHost + 5-tab MainScaffold
   ├─ theme/      # Energy design system (colors, type, shapes, motion, icons)
   ├─ components/ # MapWidget (speed-colored routes), rings, gauge, cards
   └─ screens/    # splash, auth, home, progress, workout, history, profile
```
