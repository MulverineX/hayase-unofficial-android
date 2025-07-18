import { Plugin } from "@capacitor/core"

export interface EngageAccount {
    accoundId: string
    profileId?: string
    locale?: string
}

export const PlatformType = {
    UNSPECIFIED: 0,
    ANDROID_TV: 1,
    ANDROID_MOBILE: 2,
    IOS: 3
}

export const WatchNextType = {
    UNKNOWN: 0,
    CONTINUE: 1,
    NEXT: 2,
    NEW: 3,
    WATCHLIST: 4
}

export interface EngageUri {
    uri: string
    type: number
}

export interface EngageImage {
    uri: string
    width: number
    height: number
}

export interface EngageAvailabilityWindow {
    startTimestampMillis: number
    endTimestampMillis: number
}

export interface EngageContentRating {
    rating: string
    agencyName: string
}

export type EngageEntry = EngageTvEpisodeEntry | EngageSeasonEntry | EngageShowEntry | EngageMovieEntry

export type EngageContinueEntry = EngageTvEpisodeEntry | EngageMovieEntry

export interface EngageTvEpisodeEntry {
    type: 'tv_episode'
    availabilityTimeWindows?: EngageAvailabilityWindow[]
    contentRatings?: EngageContentRating[]
    infoPageUri?: string
    entityId?: string
    watchNextType?: number // REQUIRED ONLY FOR CONTINUATION CLUSTERS
    downloadedOnDevice?: boolean
    isNextEpisodeAvailable?: boolean
    name: string
    platformSpecificPlaybackUris: EngageUri[]
    posterImages: EngageImage[]
    lastEngagementTimeMillis?: number // REQUIRED ONLY FOR CONTINUATION CLUSTERS
    durationMillis: number
    lastPlayBackPositionTimeMillis?: number // REQUIRED ONLY FOR CONTINUATION CLUSTERS WHEN WATCHTYPE IS CONTINUE
    episodeNumber: number
    seasonNumber: string
    showTitle: string
    seasonTitle: string
    airDateEpochMillis: number
    genres: string[]
}

export interface EngageSeasonEntry {
    type: 'tv_season'
    availabilityTimeWindows?: EngageAvailabilityWindow[]
    contentRatings?: EngageContentRating[]
    infoPageUri: string
    playBackUri?: string
    entityId?: string
    watchNextType?: number
    name: string
    posterImages: EngageImage[]
    lastEngagementTimeMillis?: number
    lastPlayBackPositionTimeMillis?: number
    seasonNumber: number
    genres: string[]
    firstEpisodeAirDateEpochMillis?: number
    latestEpisodeAirDateEpochMillis?: number
    episodeCount: number
}

export interface EngageShowEntry {
    type: 'tv_show'
    availabilityTimeWindows?: EngageAvailabilityWindow[]
    contentRatings?: EngageContentRating[]
    infoPageUri: string
    playBackUri?: string
    entityId?: string
    watchNextType?: number
    name: string
    posterImages: EngageImage[]
    lastEngagementTimeMillis?: number
    lastPlayBackPositionTimeMillis?: number
    genres: string[]
    firstEpisodeAirDateEpochMillis?: number
    latestEpisodeAirDateEpochMillis?: number
    seasonCount: number
}

export interface EngageMovieEntry {
    type: 'movie'
    availabilityTimeWindows?: EngageAvailabilityWindow[]
    contentRatings?: EngageContentRating[]
    genres: string[]
    platformSpecificPlaybackUris: EngageUri[]
    posterImages: EngageImage[]
    description: string
    downloadedOnDevice?: boolean
    durationMillis: number
    entityId?: string
    infoPageUri?: string
    lastEngagementTimeMillis?: number // REQUIRED ONLY FOR CONTINUATION CLUSTERS
    LastPlayBackPositionTimeMillis?: number // REQUIRED ONLY FOR CONTINUATION CLUSTERS WHEN WATCHTYPE IS CONTINUE
    name: string
    releaseDateEpochMillis: number
    watchNextType?: number // REQUIRED FOR CONTINUATION CLUSTERS ONLY
}

export interface ContinuationCluster {
    accountProfile: EngageAccount
    entries: EngageContinueEntry[]
}

export interface FeaturedCluster {
    entries: EngageEntry[]
}

export interface RecommendationCluster {
    entries: EngageEntry[]
    title?: string
    subtitle?: string
    actionText?: string
    actionUri?: string
}

export interface RecommendationClusterOptions {
    accountProfile: EngageAccount, 
    clusters: RecommendationCluster[]
}

export interface EngagePlugin extends Plugin {
    isServiceAvailable(): Promise<{result: boolean}>
    publishContinuationCluster(cluster: ContinuationCluster): Promise<void>
    publishRecommendationCluster(options: RecommendationClusterOptions): Promise<void>
    publishFeaturedCluster(cluster: FeaturedCluster): Promise<void>
    deleteContinuationCluster(): Promise<void>
    deleteFeaturedCluster(): Promise<void>
    deleteRecommendationClusters(): Promise<void>
}