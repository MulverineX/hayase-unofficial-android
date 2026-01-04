declare module 'bridge' {
  export interface BridgeChannel<T = unknown> {
    on: (event: string, listener: (data: T) => void) => void
    once: (event: string, listener: (data: T) => void) => void
    addListener: (event: string, listener: (data: T) => void) => void
    removeListener: (event: string, listener: (data: T) => void) => void
    removeAllListeners: (event?: string) => void
    send: (event: string, ...args: T[]) => void
  }
  export const channel: BridgeChannel
  export function getDataPath(): string
  export function onPause(listener: () => void): string
  export function onResume(listener: () => void): string
}
