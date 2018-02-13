package cz.muni.fi.kurcik.kgs.linkmining.ranking;

/**
 * Ranking function used for link miner
 * @author Lukáš Kurčík
 */
public interface Ranking {
    /**
     * Ranking function for number of ingoing and outgoing links
     * @param in number of links to this url
     * @param out number of links from this url
     * @return computed result
     */
    double linkRank(long in, long out);

    /**
     * Ranking function for distance from cluster.
     * @param distance Distance from cluster
     * @param rankingPoint mean or median distance of all links from cluster
     * @return computed result
     */
    double distanceRank(double distance, double rankingPoint);

    /**
     * Basic ranking if there is link between 2 urls
     * @return computed result
     */
    double haveLinkRank();

    /**
     * Rank for default document cluster
     * @return computed result
     */
    double clusterRank();
}
