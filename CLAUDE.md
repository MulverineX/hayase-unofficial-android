# Hayase Unofficial Android Build

Capacitor-based Android application that runs a Node.js runtime for torrent functionality.

## Architecture

### Layers
- **Web Layer** (`src/preload.ts`): Capacitor app running in WebView
- **Node.js Layer** (`src/background.ts`): Node.js runtime for torrent client
- **Native Layer**: Android with Capacitor plugins

### Build Output
- `build/preload.js` - Web layer (compiled from `src/preload.ts`)
- `build/nodejs/index.js` - Node.js layer (compiled from `src/background.ts`)

## Dependencies

### capacitor-nodejs
**Source**: `https://github.com/hampoelz/Capacitor-NodeJS`
**Install**: GitHub release tarball (not published to npm)

To update the version, get the tarball URL from:
https://github.com/hampoelz/Capacitor-NodeJS/releases

Current version in package.json:
```
"capacitor-nodejs": "https://github.com/hampoelz/capacitor-nodejs/releases/download/v1.0.0-beta.9/capacitor-nodejs.tgz"
```

The package bundles:
- `libnode.so` binaries for Android (arm64-v8a, armeabi-v7a, x86_64)
- Node.js headers for native module compilation
- `bridge` module for web-to-Node.js communication

#### Web Side API (src/preload.ts)
```typescript
import { NodeJS } from 'capacitor-nodejs'

// Wait for Node.js runtime to initialize
await NodeJS.whenReady()

// Send message to Node.js
NodeJS.send({ eventName: 'port-init', args: [data] })

// Listen for events from Node.js
const handle = await NodeJS.addListener('eventName', (event) => {
  // event.args contains the arguments
})

// Remove listener
NodeJS.removeListener(handle)
```

#### Node.js Side API (src/background.ts)
```typescript
import { channel } from 'bridge'

// Listen for events from web
channel.on('port-init', (data) => { ... })

// Send message to web
channel.send('message', data)

// Remove listener
channel.removeListener('event', listener)
```

## Build Process

```bash
# Build native modules for Android (uses Docker)
npm run build:native

# Build web and Node.js bundles
npm run build:web

# Generate assets
npm run build:assets

# Full app build
npm run build:app
```

### Native Module Build
The `public/nodejs/` directory contains:
- `package.json` - Node.js dependencies (e.g., `utp-native`)
- `Dockerfile` - Build environment with Android NDK
- `setup-deps.sh` - Compiles native modules for arm, arm64, x64

Native modules are built using headers bundled with `capacitor-nodejs` at:
`node_modules/capacitor-nodejs/android/libnode/include/`

## Key Files

| File | Purpose |
|------|---------|
| `src/preload.ts` | Web layer - Capacitor plugins, UI bridge |
| `src/background.ts` | Node.js layer - TorrentClient |
| `src/types.d.ts` | Type declarations for `bridge` module |
| `webpack.config.js` | Builds both layers |
| `public/nodejs/` | Node.js runtime dependencies |
