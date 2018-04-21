package cz.muni.fi.kurcik.kgs.clustering.util;

/**
 * Computes maximum number of clusters using technique from original Kiwi Application
 *
 * @author Lukáš Kurčík
 */
public class LogClusterNumber implements ClusterNumber {

    /**
     * Computes number of clusters from number of documents
     *
     * @param documents number of documents
     * @return number of clusters
     */
    @Override
    public int compute(int documents) {
        if (documents < 30)
            return documents;
        return Double.valueOf(
                Math.log(1 + documents * documents) / Math.log(3 / 2) + 3 * Math.floorDiv(documents, 200)
        ).intValue();
    }
}
