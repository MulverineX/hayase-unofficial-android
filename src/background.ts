import { env } from 'node:process'

import { expose, type Endpoint, transferHandlers, proxyMarker, isObject } from 'abslink'
import { type BridgeChannel, channel } from 'bridge'
import TorrentClient from 'torrent-client'

import type { TorrentSettings } from 'native'

function createWrapper <T> (c: BridgeChannel<T>): Endpoint {
  return {
    on (event: string, listener: (data: T) => void) {
      c.on(event, listener)
    },
    off (event: string, listener: (...args: any[]) => void) {
      c.removeListener(event, listener)
    },
    postMessage (message: T) {
      c.send('message', message)
    }
  }
}

transferHandlers.set('error', {
  canHandle: (value): value is Error => isObject(value) && value instanceof Error && !(proxyMarker in value),
  serialize: (value: unknown) => ({ message: value.message, name: value.name, stack: value.stack }),
  deserialize: (serialized: unknown) => Object.assign(new Error(serialized.message), serialized)
})

let tclient: TorrentClient | undefined

channel.on('port-init', _data => {
  let settings: TorrentSettings & { path: string } | undefined
  const { id, data } = _data as { id: string, data: unknown}
  if (id === 'settings' || id === 'init') settings = data as TorrentSettings & { path: string }
  if (id === 'destroy') tclient?.destroy()

  if (id === 'init') {
    tclient ??= new TorrentClient(settings!, env.TMPDIR!)
    // re-exposing leaks memory, but not that much, so it's fine
    expose(tclient, createWrapper(channel))
  } else if (settings) {
    tclient?.updateSettings(settings)
  }
})
