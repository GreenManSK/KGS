package cz.muni.fi.kurcik.kgs.clustering;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface to cluster data from preprocessing
 * @author Lukáš Kurčík
 */
public interface Clustering {
    String CLUSTERING_FILES_DIR = "clustering";
    String VOCAB_FILE = "vocab.txt";
    String CORPUS_FILE = "corpus.dat";
    String CLUSTERING_FILE = "clusters.txt";

    /**
     * Takes prerocessed data from CLUSTERING_FILES_DIR and makes clusters from them.
     * Saves list of topic probabilities for each document into file and each cluster into separate file
     * @throws IOException when there is problem with file IO
     */
    void cluster() throws IOException;

    /**
     * Sets download directory for downloader. All data will be put into dirName/url
     *
     * @param dir Directory to download folder
     */
    void setDownloadDirectory(Path dir);

    /**
     * Returns path to folder with downloaded data
     *
     * @return directory to download folder
     */
    Path getDownloadDirectory();
}
