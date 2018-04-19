package cz.muni.fi.kurcik.kgs.clustering;

import cz.muni.fi.kurcik.kgs.util.Module;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface to cluster data from preprocessing
 * @author Lukáš Kurčík
 */
public interface Clustering extends Module {
    String CLUSTERING_FILES_DIR = "clustering";
    String VOCAB_FILE = "vocab.txt";
    String CORPUS_FILE = "corpus.dat";
    String CLUSTERING_FILE = "clusters.txt";
    String URL_CLUSTER_FILE = "url-cluster.txt";

    /**
     * Takes prerocessed data from CLUSTERING_FILES_DIR and makes clusters from them.
     * Saves list of topic probabilities for each document into file and each cluster into separate file
     * Saves all url-cluster pairs into URL_CLUSTER_FILE
     * @todo: Saves all url-cluster pairs into URL_CLUSTER_FILE
     * @throws IOException when there is problem with file IO
     */
    void cluster() throws IOException;
}
