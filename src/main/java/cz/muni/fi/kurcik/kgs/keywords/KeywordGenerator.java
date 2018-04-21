package cz.muni.fi.kurcik.kgs.keywords;

import cz.muni.fi.kurcik.kgs.util.Module;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for keywords generators
 *
 * @author Lukáš Kurčík
 */
public interface KeywordGenerator extends Module {

    String KEYWORDS_FILES_DIR = "keywords";

    /**
     * Generates keywords for each cluster and saves them into KEYWORDS_FILES_DIR, each cluster will have separate file.
     * Each keyword on separate line.
     *
     * @param clusteringFile Path to clustering file
     * @param keywordsForCluster Number of keywords for each cluster to generate
     * @throws IOException when there is problem with file IO
     */
    void generateKeywords(Path clusteringFile, int keywordsForCluster) throws IOException;
}
