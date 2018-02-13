package cz.muni.fi.kurcik.kgs.linkmining.ranking;

/**
 * Simplified PageRank based on Wiki system from Vladimír Matejovský
 * (4 * log(15))/log(out + 15) - 1
 * @author Lukáš Kurčík
 */
public class SimplifiedPageRank implements Ranking {
    /**
     * Ranking function
     *
     * @param in  number of links to this url
     * @param out number of links from this url
     * @return computed result
     */
    @Override
    public double linkRank(long in, long out) {
        return (4 * Math.log(15)) / Math.log(out + 15) - 1;
    }

    /**
     * Ranking function for distance from cluster.
     *
     * @param distance     Distance from cluster
     * @param rankingPoint mean or median distance of all links from cluster
     * @return computed result
     */
    @Override
    public double distanceRank(double distance, double rankingPoint) {
        return distance > rankingPoint ? 1 : 2;
    }

    /**
     * Basic ranking if there is link between 2 urls
     *
     * @return computed result
     */
    @Override
    public double haveLinkRank() {
        return 1;
    }

    @Override
    public double clusterRank() {
        return 0;
    }
}
