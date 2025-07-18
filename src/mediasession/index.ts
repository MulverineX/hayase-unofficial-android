import { registerPlugin } from "@capacitor/core";
import { MediaSessionPlugin, MediaState, MediaMetadata } from "./definitions";
import { SessionMetadata } from "native";

const mediaSessionPlugin = registerPlugin<MediaSessionPlugin>('MediaNotification', {});

let lastKnownMetadata: MediaMetadata | null = null;
let lastKnownState: MediaState | null = null;

interface MediaSessionPluginExports {
    setMediaSession: (metadata: SessionMetadata, mediaId: number) => Promise<void>
    setPositionState: (state?: MediaPositionState) => Promise<void>
    setPlayBackState: (paused: 'none' | 'paused' | 'playing') => Promise<void>
    setActionHandler: (action: MediaSessionAction | 'enterpictureinpicture', handler: MediaSessionActionHandler | null) => void
}

const stateMapping = {
    'none': 0,
    'stopped': 1,
    'paused': 2,
    'playing': 3,
    'fast_forwarding': 4,
    'rewinding': 5,
    'buffering': 6,
    'error': 7,
    'connecting': 8,
    'skipping_to_previous': 9,
    'skipping_to_next': 10,
    'skipping_to_queue_item': 11,
    'position_unknown': -1
}

function stateFor(value: keyof typeof stateMapping) {
    if (Object.keys(stateMapping).includes(value)) {
        return stateMapping[value]
    }
    throw new Error("Unknown state "+value)
}

const defaultExport: MediaSessionPluginExports = {
    setMediaSession: async (metadata, mediaId) => {
        if (lastKnownMetadata == null) {
            lastKnownMetadata = {duration: 0, ...metadata}
        } else {
            lastKnownMetadata = {...lastKnownMetadata, ...metadata}
        }
        console.log("Metadata updated")
        await mediaSessionPlugin.setMediaSession(lastKnownMetadata!)
    },
    setPositionState: async (state?: MediaPositionState) => {
        if (!state) return;
        if (state.duration && lastKnownMetadata != null && lastKnownMetadata.duration != state.duration) {
            lastKnownMetadata.duration = state.duration;
            await mediaSessionPlugin.setMediaSession(lastKnownMetadata);
        }
        if (!lastKnownState) {
            lastKnownState = {state: stateMapping.none, position: state.position ?? 0, playbackRate: state.playbackRate ?? 1}
        } else {
            if (state.playbackRate != null) {
                lastKnownState.playbackRate = state.playbackRate
            }
            if (state.position != null) {
                lastKnownState.position = state.position ?? 0
            }
        }
        console.log("Position updated")
        await mediaSessionPlugin.setPlaybackState(lastKnownState)
    },
    setPlayBackState: async (paused: 'none' | 'paused' | 'playing') => {
        if (!lastKnownState) {
            lastKnownState = {state: stateFor(paused), position: 0, playbackRate: 1}
        } else {
            lastKnownState.state = stateFor(paused)
        }
        console.log("Playback updated")
        await mediaSessionPlugin.setPlaybackState(lastKnownState)
    },
    setActionHandler: async (action: MediaSessionAction | 'enterpictureinpicture', handler: MediaSessionActionHandler | null) => {
        console.log("Added Listener", action)
        mediaSessionPlugin.addListener(action, (e)=>{
            console.log("Fired event", action, e)
            handler&&handler({action, ...e})
        })
    }
}

export default defaultExport