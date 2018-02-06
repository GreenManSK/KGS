package cz.muni.fi.kurcik.kgs.clustering;

/**
 * Interface for working with distances in clustering model
 * Implementation should know number of clusters and have data
 * about documents to compute distances from clusters.
 *
 * @author Lukáš Kurčík
 */
public interface DistanceModel {
    /**
     * Return distance of document from centroid of cluster
     * @param clusterId cluster id
     * @param documentId document id
     * @return distance from cluster centroid
     */
    double centroidDistance(int clusterId, int documentId);

    /**
     * Return mean distance of all documents in this cluster from it's centroid
     * @param clusterId cluster id
     * @return mean distance of documents from centroid
     */
    double meanDistance(int clusterId);

    /**
     * Return median distance of all documents in this cluster from it's centroid
     * @param clusterId cluster id
     * @return median distance of documents from centroid
     */
    double medianDistance(int clusterId);
}
