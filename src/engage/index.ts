import { registerPlugin } from "@capacitor/core";
import { ContinuationCluster, EngagePlugin, FeaturedCluster, RecommendationClusterOptions } from "./definitions";

const engagePlugin = registerPlugin<EngagePlugin>('Engage', {});

export default {
    isServiceAvailable: async ()=>(await engagePlugin.isServiceAvailable()).result,
    publishContinuationCluster: async (cluster: ContinuationCluster) => await engagePlugin.publishContinuationCluster(cluster),
    publishRecommendationCluster: async (options: RecommendationClusterOptions) => await engagePlugin.publishRecommendationCluster(options),
    publishFeaturedCluster: async (cluster: FeaturedCluster) => await engagePlugin.publishFeaturedCluster(cluster),
    deleteContinuationCluster: async () => await engagePlugin.deleteContinuationCluster(),
    deleteFeaturedCluster: async () => await engagePlugin.deleteFeaturedCluster(),
    deleteRecommendationClusters: async () => await engagePlugin.deleteRecommendationClusters()
}