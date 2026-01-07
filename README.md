# Unofficial build of Hayase app for Android

## NOTICE: do NOT go to upstream/hayase/thaunknown for support

These builds are provided without any support, feel free to open an issue but it will most likely be ignored.

Currently subtitles on legacy devices (including the Nvidia Shield) do not work due to upstream's dependency on WebGPU. \
I am working on a fork of JASSUB to solve this issue [here](https://github.com/MulverineX/jassub-compat).

In the future I may add a patch that disables the useless network checks during app setup.

Yes, I had the option of sleuthing around the Hayase API endpoint, but I figured this is more future proof and useful.

## Changes from upstream

- Disables Updater
- Adds missing android resources
- Skips incompatible node-datachannel module, not a problem because the import for it is disabled upstream anyway
- Removes support for NZB/Usenet because something wasn't working
- Fixes torrent tracker JS bundle path resolution within the GH:A CI environment
- Adds explicit `ndk.abiFilters` to get Capacitor-NodeJS working on Android TV
- Replaces the two closed-source dependencies with an open-source alternative:

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