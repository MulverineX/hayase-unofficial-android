import { env } from 'node:process'

import { expose, type Endpoint } from 'abslink'
import { type BridgeChannel, channel } from 'bridge'
import TorrentClient from 'torrent-client'

import './serializers/error'

import type { TorrentSettings } from 'native'

// Debug logging helper
const DEBUG_TAG = '[HAYASE-NODE]'
function debugLog (...args: unknown[]) {
  console.log(DEBUG_TAG, ...args)
}

debugLog('Node.js layer initializing...')
debugLog('TMPDIR:', env.TMPDIR)
debugLog('Node version:', process.version)
debugLog('Platform:', process.platform, 'Arch:', process.arch)

debugLog('Creating bridge wrapper...')

function createWrapper <T> (c: BridgeChannel<T>): Endpoint {
  return {
    on (event: string, listener: (data: T) => void) {
      debugLog('Bridge wrapper: registering listener for event:', event)
      c.on(event, listener)
    },
    off (event: string, listener: (...args: any[]) => void) {
      debugLog('Bridge wrapper: removing listener for event:', event)
      c.removeListener(event, listener)
    },
    postMessage (message: T) {
      debugLog('Bridge wrapper: posting message')
      c.send('message', message)
    }
  }
}

debugLog('Bridge wrapper created')

let tclient: TorrentClient | undefined

debugLog('Registering channel listeners...')

channel.on('port-init', _data => {
  debugLog('Received port-init event:', JSON.stringify(_data))
  let settings: TorrentSettings & { path: string } | undefined
  const { id, data } = _data as { id: string, data: unknown}
  debugLog('port-init id:', id)
  if (id === 'settings' || id === 'init') settings = data as TorrentSettings & { path: string }
  if (id === 'destroy') {
    debugLog('Destroying torrent client from port-init')
    tclient?.destroy()
  }

  if (id === 'init') {
    debugLog('Initializing TorrentClient with TMPDIR:', env.TMPDIR)
    debugLog('TorrentClient settings:', JSON.stringify(settings))
    try {
      tclient ??= new TorrentClient(settings!, env.TMPDIR!)
      debugLog('TorrentClient created successfully')
      // re-exposing leaks memory, but not that much, so it's fine
      expose(tclient, createWrapper(channel))
      debugLog('TorrentClient exposed via bridge')
    } catch (err) {
      debugLog('ERROR creating TorrentClient:', err)
    }
  } else if (settings) {
    debugLog('Updating TorrentClient settings')
    tclient?.updateSettings(settings)
  }
})

channel.on('destroy', () => {
  debugLog('Received destroy event')
  tclient?.destroy()
})

debugLog('Channel listeners registered, Node.js layer ready')
