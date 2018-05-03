package cz.muni.fi.kurcik.kgs.clustering.util;

/**
 * Computes maximum number of topics using technique based on the original Kiwi Application
 *
 * @author Lukáš Kurčík
 */
public class LogTopicNumber implements ClusterNumber {

    /**
     * Computes number of clusters from number of documents
     *
     * @param documents number of documents
     * @return number of clusters
     */
    @Override
    public int compute(int documents) {
        if (documents < 30)
            return 3 * documents;
        return 3 * Double.valueOf(
                Math.log(1 + documents * documents) / Math.log(3.0D / 2.0D) + 3 * Math.floorDiv(documents, 200)
        ).intValue();
    }
}
