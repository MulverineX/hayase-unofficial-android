const mode = process.env.NODE_ENV?.trim() ?? 'development'

module.exports = {
  appId: 'watch.miru',
  appName: 'Hayase',
  webDir: 'build',
  bundledWebRuntime: false,
  loggingBehavior: 'none',
  android: {
    buildOptions: {
      keystorePath: './watch.miru',
      keystorePassword: '',
      keystoreAlias: 'watch.miru'
    },
    webContentsDebuggingEnabled: mode === 'development'
  },
  plugins: {
    SplashScreen: { launchShowDuration: 0 },
    CapacitorHttp: { enabled: false },
    CapacitorNodeJS: { nodeDir: 'nodejs' }
  },
  server: {
    cleartext: true,
    // url: mode === 'development' ? 'http://localhost:5001' : 'https://hayase.app'
    url: 'https://hayase.app'
  }
}
