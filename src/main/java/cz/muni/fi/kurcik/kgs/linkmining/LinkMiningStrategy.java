package cz.muni.fi.kurcik.kgs.linkmining;

/**
 * Specify strategy for computing link mining values
 */
public enum LinkMiningStrategy {
    /**
     * Use mean distance from clusters
     */
    MEAN,
    /**
     * Use median of all distances from cluster
     */
    MEDIAN;
}
