package cz.muni.fi.kurcik.kgs.clustering;

/**
 * Representation of fuzzy clustering model
 *
 * @author Lukáš Kurčík
 */
public interface FuzzyClusteringModel extends ClusteringModel {

    /**
     * Return cluster for document
     * @param documentId document id
     * @return cluster id
     */
    int getCluster(int documentId);

    /**
     * Return probability matrix of document
     * @param documentId document id
     * @return array of probabilities for each cluster
     */
    double[] getClusterMatrix(int documentId);

    /**
     * Return probability matrix of cluster
     * @param clusterId cluster id
     * @return array of probabilities for each document
     */
    double[] getDocumentMatrix(int clusterId);

    /**
     * Return probability matrix for clustering
     * @return [documentId][clusterId]
     */
    double[][] getProbabilityMatrix();

    /**
     * Return number of clusters
     * @return number of clusters
     */
    int getClusterCount();
}
