/* globals navigationbar, PictureInPicture */
import { App } from '@capacitor/app'
import { Browser } from '@capacitor/browser'
import { Device } from '@capacitor/device'
// import { LocalNotifications } from '@capacitor/local-notifications'
import { Filesystem } from '@capacitor/filesystem'
import { Share } from '@capacitor/share'
import { StatusBar, Style } from '@capacitor/status-bar'
import { proxy, transferHandlers, proxyMarker, wrap as _wrap, type Endpoint, type Remote, isObject } from 'abslink'
import { FolderPicker } from 'capacitor-folder-picker'
import { IntentUri } from 'capacitor-intent-uri'
import { type ChannelListenerCallback, NodeJS } from 'capacitor-nodejs'
// import { SafeArea } from 'capacitor-plugin-safe-area'

import type { PluginListenerHandle } from '@capacitor/core'
import type { Native } from 'native'
import type TorrentClient from 'torrent-client'

// @ts-expect-error yep.
if (!window.native) {
  // window.WatchNext.addOrUpdateWatchNext(
  //   'uniqueId1234',
  //   'Gachiakuta',
  //   '2. The Inhabited',
  //   'https://artworks.thetvdb.com/banners/v4/episode/11180307/screencap/686fa690ebef0.jpg',
  //   7200000,
  //   3600000,
  //   'intent:#Intent;action=android.intent.action.VIEW;data=hayase://playback/uniqueId1;package=watch.miru;end'
  // )

  // window.WatchNext.addTrendingRecommendation(
  //   '', // channelId (empty to auto-create)
  //   'uniqueId2', // id
  //   'Trending Show', // title
  //   'Everyone is watching this!', // description
  //   'https://i.ytimg.com/vi/nEj2X9x9M7Q/maxresdefault.jpg', // posterArtUri
  //   'hayase://playback/uniqueId2' // intentUri
  // )

  // window.WatchNext.addPlannedRecommendation(
  //   '', // channelId (empty to auto-create)
  //   'uniqueId3', // id
  //   'Planned Show', // title
  //   'You planned to watch this.', // description
  //   'https://i.ytimg.com/vi/dYF8mVz0vIA/maxresdefault.jpg', // posterArtUri
  //   'hayase://playback/uniqueId3' // intentUri
  // )
  // let canShowNotifications = false

  // LocalNotifications.checkPermissions().then(async value => {
  //   if (value) {
  //     try {
  //       await LocalNotifications.requestPermissions()
  //       canShowNotifications = true
  //     } catch (e) {
  //       console.error(e)
  //     }
  //   }
  // })

  // let id = 0
  // IPC.on('notification', noti => {
  //   /** @type {import('@capacitor/local-notifications').LocalNotificationSchema} */
  //   const notification = {
  //     title: noti.title,
  //     body: noti.body,
  //     id: id++,
  //     attachments: [
  //       {
  //         id: '' + id++,
  //         url: noti.icon
  //       }
  //     ]
  //   }
  //   if (canShowNotifications) LocalNotifications.schedule({ notifications: [notification] })
  // })

  transferHandlers.set('error', {
    canHandle: (value): value is Error => isObject(value) && value instanceof Error && !(proxyMarker in value),
    serialize: (value: unknown) => ({ message: value.message, name: value.name, stack: value.stack }),
    deserialize: (serialized: unknown) => Object.assign(new Error(serialized.message), serialized)
  })

  const protocolRx = /hayase:\/\/([a-z0-9]+)\/(.*)/i

  function _parseProtocol (text: string) {
    const match = text.match(protocolRx)
    if (!match) return null
    return {
      target: match[1]!,
      value: match[2]
    }
  }

  function handleProtocol (text: string) {
    const parsed = _parseProtocol(text)
    if (!parsed) return
    if (parsed.target === 'donate') Browser.open({ url: 'https://github.com/sponsors/ThaUnknown/' })

    return parsed
  }

  // SafeArea.addListener('safeAreaChanged', updateInsets)
  // screen.orientation.addEventListener('change', updateInsets)

  // async function updateInsets () {
  //   const { insets } = await SafeArea.getSafeAreaInsets()
  //   for (const [key, value] of Object.entries(insets)) {
  //     document.documentElement.style.setProperty(`--safe-area-${key}`, `${value}px`)
  //   }
  // }
  // updateInsets()

  StatusBar.hide()
  StatusBar.setStyle({ style: Style.Dark })
  StatusBar.setOverlaysWebView({ overlay: true })

  // @ts-expect-error global
  navigationbar.setUp(true)

  // cordova screen orientation plugin is also used, and it patches global screen.orientation.lock

  // hook into pip request, and use our own pip implementation, then instantly report exit pip
  // this is more like DOM PiP, rather than video PiP
  HTMLVideoElement.prototype.requestPictureInPicture = function () {
    // @ts-expect-error global
    PictureInPicture.enter(this.videoWidth, this.videoHeight, success => {
      this.dispatchEvent(new Event('leavepictureinpicture'))
      if (success) document.querySelector('#episodeListTarget')?.requestFullscreen()
    }, err => {
      this.dispatchEvent(new Event('leavepictureinpicture'))
      console.error(err)
    })

    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this

    return Promise.resolve({
      addEventListener: () => {},
      removeEventListener: () => {},
      get width () { return self.videoWidth },
      get height () { return self.videoHeight },
      onresize: null,
      dispatchEvent: () => false
    })
  }

  function createWrapper (channel: typeof NodeJS): Endpoint {
    const listeners = new WeakMap<(...args: any[]) => void, PluginListenerHandle>()

    return {
      on (event: string, listener: (data: unknown) => void) {
      // @ts-expect-error idfk
        const unwrapped: ChannelListenerCallback = (event) => listener(...event.args)
        listeners.set(listener, channel.addListener(event, unwrapped))
      },
      off (event: string, listener: (...args: any[]) => void) {
        const unwrapped = listeners.get(listener)!
        channel.removeListener(unwrapped)

        listeners.delete(listener)
      },
      postMessage (message: unknown) {
        channel.send({ eventName: 'message', args: [message] })
      }
    }
  }

  function wrap<T> (): Remote<T> {
    return _wrap(createWrapper(NodeJS))
  }

  console.warn('loaded native')

  const DEFAULTS = {
    player: '',
    torrentPath: '',
    torrentSettings: {
      torrentPersist: false,
      torrentDHT: false,
      torrentStreamedDownload: true,
      torrentSpeed: 40,
      maxConns: 50,
      torrentPort: 0,
      dhtPort: 0,
      torrentPeX: false
    }
  }

  class Store {
    data = DEFAULTS
    constructor () {
      this.data = parseDataFile()
    }

    get<K extends keyof Store['data']> (key: K): Store['data'][K] {
      return this.data[key]
    }

    set<K extends keyof Store['data']> (key: K, val: Store['data'][K]) {
      this.data[key] = val
      localStorage.setItem('userData', JSON.stringify(this.data))
    }
  }

  function parseDataFile () {
    try {
      return { ...DEFAULTS, ...(JSON.parse(localStorage.getItem('userData') ?? '') as typeof DEFAULTS) }
    } catch (error) {
      console.error('Failed to load native settings: ', error)
      return DEFAULTS
    }
  }

  const store = new Store()

  const STORAGE_TYPE_MAP = {
    primary: '/sdcard/',
    secondary: '/sdcard/'
  }

  // might be required in folder picker plugin
  // final int takeFlags = intent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  // getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);

  const torrent = NodeJS.whenReady().then(async () => {
    if (store.data.torrentPath) await Filesystem.requestPermissions()
    NodeJS.send({ eventName: 'port-init', args: [{ id: 'init', data: { ...store.data.torrentSettings, path: store.data.torrentPath } }] })
    return wrap<TorrentClient>()
  })
  const version = App.getInfo().then(info => info.version)

  const native: Partial<Native> = {
    openURL: (url: string) => Browser.open({ url }),
    selectDownload: async () => {
      const result = await FolderPicker.chooseFolder() as unknown as { path: string }
      const normalizedPath = decodeURIComponent(result.path)

      const [, uri, ...path] = normalizedPath.split(':')
      const [,, app, subpath, type, ...rest] = uri!.split('/')

      if (app !== 'com.android.externalstorage.documents') throw new Error('Unverified app', { cause: 'Expected com.android.externalstorage.documents, got: ' + app })
      if (rest.length) throw new Error('Unsupported uri', { cause: 'Unxpected access type, got: tree/' + rest.join('/') })
      if (subpath !== 'tree') throw new Error('Unsupported subpath type', { cause: 'Expected tree subpath, got: ' + subpath })

      let base = STORAGE_TYPE_MAP[type as keyof typeof STORAGE_TYPE_MAP]
      if (!base) {
        if (!/[a-z0-9]{4}-[a-z0-9]{4}/i.test(type!)) throw new Error('Unsupported storage type')
        base = `/storage/${type}/`
      }

      const respath = base + path.join('')

      store.set('torrentPath', respath)

      return respath
    },
    // getLogs: () => main.getLogs(),
    getDeviceInfo: async () => ({
      features: {},
      info: await Device.getInfo(),
      cpu: {},
      ram: {}
    }),
    setActionHandler: (action, cb) => undefined,
    setMediaSession: async (metadata, id) => undefined,
    setPositionState: async e => undefined,
    setPlayBackState: async e => undefined,
    checkAvailableSpace: async () => await (await torrent).checkAvailableSpace(),
    checkIncomingConnections: async (port) => await (await torrent).checkIncomingConnections(port),
    updatePeerCounts: async (hashes) => await (await torrent).scrape(hashes),
    playTorrent: async (id, mediaID, episode) => await (await torrent).playTorrent(id, mediaID, episode),
    library: async () => await (await torrent).library(),
    attachments: async (hash, id) => await (await torrent).attachments.attachments(hash, id),
    tracks: async (hash, id) => await (await torrent).attachments.tracks(hash, id),
    subtitles: async (hash, id, cb) => await (await torrent).attachments.subtitle(hash, id, proxy(cb)),
    errors: async (cb) => await (await torrent).errors(proxy(cb)),
    chapters: async (hash, id) => await (await torrent).attachments.chapters(hash, id),
    torrentInfo: async (hash) => await (await torrent).torrentInfo(hash),
    peerInfo: async (hash) => await (await torrent).peerInfo(hash),
    fileInfo: async (hash) => await (await torrent).fileInfo(hash),
    protocolStatus: async (hash) => await (await torrent).protocolStatus(hash),
    updateSettings: async (settings) => {
      store.set('torrentSettings', settings)
      await torrent
      NodeJS.send({ eventName: 'port-init', args: [{ id: 'settings', data: { ...store.data.torrentSettings, path: store.data.torrentPath } }] })
    },
    cachedTorrents: async () => await (await torrent).cached(),
    isApp: true,
    spawnPlayer: async (url) => {
      const res = await IntentUri.openUri({ url: `${url.replace('http', 'intent')}#Intent;type=video/any;scheme=http;end;` })
      if (!res.completed) throw new Error(res.message)
    },
    setDOH: async () => {
      const res = await IntentUri.openUri({ url: 'intent:#Intent;action=android.settings.SETTINGS;end;' })
      if (!res.completed) throw new Error(res.message)
    },
    version: () => version,
    navigate: async (cb) => {
      App.addListener('appUrlOpen', ({ url }) => {
        const res = handleProtocol(url)
        if (res) cb(res)
      })
      const url = await App.getLaunchUrl()
      if (!url) return
      const res = handleProtocol(url.url)
      if (res) cb(res)
    },
    share: async (data) => {
      if (!data) return
      Share.share({ title: data.title, url: data.url, dialogTitle: data.title })
    },
    defaultTransparency: () => false,
    debug: async (levels) => await (await torrent).debug(levels)
  }

  // @ts-expect-error yep.
  window.native = native
}
