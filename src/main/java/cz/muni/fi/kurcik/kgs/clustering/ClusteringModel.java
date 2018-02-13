package cz.muni.fi.kurcik.kgs.clustering;

/**
 * Representation of hard clustering model
 *
 * @author Lukáš Kurčík
 */
public interface ClusteringModel {
    /**
     * Return cluster for document
     * @param documentId document id
     * @return cluster id
     */
    int getCluster(int documentId);

    /**
     * Return array of ids of all documents in this cluster
     * @param clusterId cluster id
     * @return array of ids
     */
    int[] getDocuments(int clusterId);

    /**
     * Return number of clusters
     * @return number of clusters
     */
    int getClusterCount();

    /**
     * Return number of documents
     * @return number of documents
     */
    int getDocumentCount();
}
