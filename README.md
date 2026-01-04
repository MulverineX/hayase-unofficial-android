# Unofficial build of Hayase app for Android

## Changes from upstream

This fork replaces closed-source dependencies with open-source alternatives:

### capacitor-nodejs

| | Before | After |
|-|--------|-------|
| **Source** | `hayase-app/capacitor-nodejs` (closed source) | [`hampoelz/Capacitor-NodeJS`](https://github.com/hampoelz/Capacitor-NodeJS) (MIT) |
| **Install** | Git URL | GitHub release tarball |
| **Node.js version** | Unknown | 18.20.4 |

The replacement package bundles:
- Pre-compiled `libnode.so` binaries for Android (arm64-v8a, armeabi-v7a, x86_64)
- Node.js headers for native module compilation
- `bridge` module for Capacitor-to-Node.js communication

### nodejs-mobile submodule

| | Before | After |
|-|--------|-------|
| **Submodule** | `thaunknown/nodejs-mobile` | Removed |
| **Headers** | From submodule | Bundled in `capacitor-nodejs` |

The nodejs-mobile git submodule has been removed entirely. The `capacitor-nodejs` package now provides all necessary headers for building native Node.js modules, eliminating the need for an external submodule.

## Build

### Prerequisites

- Node.js 22+
- pnpm 9+
- Docker (for native module compilation)
- Android SDK with NDK

### Commands

```bash
# Install dependencies
pnpm install

# Build everything (web, native modules, assets)
pnpm run build:app

# Sync with Android project
npx cap sync android

# Build APK
cd android && ./gradlew assembleDebug
```

### Individual build steps

```bash
# Build web bundles only
pnpm run build:web

# Build native modules only (requires Docker)
pnpm run build:native

# Generate app icons and splash screens
pnpm run build:assets
```

## CI/CD

The GitHub Actions workflow (`.github/workflows/build.yml`) automatically:
1. Installs dependencies with pnpm
2. Builds web and native modules
3. Compiles the Android APK
4. Creates a GitHub release with the APK attached

Builds are triggered on pushes to `main` or manually via workflow dispatch.
