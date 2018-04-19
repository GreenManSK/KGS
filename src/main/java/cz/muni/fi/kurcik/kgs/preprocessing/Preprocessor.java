package cz.muni.fi.kurcik.kgs.preprocessing;

import cz.muni.fi.kurcik.kgs.clustering.corpus.Corpus;
import cz.muni.fi.kurcik.kgs.util.Module;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for preprocessing of downloaded data
 * <p>
 * Should be able to create files with normalized words from parsed files and prepare them for clustering.
 *
 * @author Lukáš Kurčík
 */
public interface Preprocessor extends Module {

    String NORMALIZED_FILES_DIR = "normalized";
    String NORMALIZED_FILE_EXTENSION = ".txt";

    /**
     * Takes all files from Downloader.PARSED_FILES_DIR, takes words from them, normalizes them and saves them into NORMALIZED_FILES_DIR.
     *
     * @throws IOException when there is problem with file IO
     */
    void normalizeParsedFiles() throws IOException;

    /**
     * Creates two files in Clustering.CLUSTERING_FILES_DIR.
     * vocab.txt contains all words from all documents. Each word on one line. Line number indicate word ID, lines are numbered from 0.
     * corpus.dat with data about all documents. Each line is in format
     * File formats are described in Corpus and Vocabulary classes
     *
     * @param corpus Corpus builder
     * @throws IOException when there is problem with file IO
     */
    void prepareClusteringFiles(Corpus corpus) throws IOException;
}
