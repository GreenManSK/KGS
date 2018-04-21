package cz.muni.fi.kurcik.kgs.clustering.util;

/**
 * Interface for computing number of clusters
 * @author Lukáš Kurčík
 */
@FunctionalInterface
public interface ClusterNumber {
    /**
     * Computes number of clusters from number of documents
     * @param documents number of documents
     * @return number of clusters
     */
    int compute(int documents);
}
