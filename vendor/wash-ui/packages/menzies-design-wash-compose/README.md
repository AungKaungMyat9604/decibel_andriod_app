# Menzies Design Wash UI Compose

Compose Multiplatform port of [menzies-design-wash-ui](../menzies-design-wash-ui/) with 60 pigment themes, custom WashTheme, and an Android demo gallery.

## Modules

| Path | Role |
|------|------|
| `packages/menzies-design-wash-compose` | KMP library (`com.mariesta.menzies.washui`) |
| `apps/demo-android` | Android demo app mirroring the web gallery |

## Build

```bash
# From repo root (requires Android SDK in local.properties)
./gradlew :demo-android:assembleDebug
./gradlew :menzies-design-wash-compose:check
npm run build:compose
```

## Theme tokens

Regenerate Kotlin color schemes from web CSS:

```bash
python3 scripts/generate_wash_compose_themes.py
```

## Targets

| Target | Status |
|--------|--------|
| Android | Runnable demo |
| Desktop (JVM) | Compile + preview `main()` |
| JS | Compile-only |
| iOS | Compile in CI on macOS |

## License

GPL-3.0-or-later (matches monorepo)
