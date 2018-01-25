package cz.muni.fi.kurcik.kgs.preprocessing;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for preprocessing of downloaded data
 * <p>
 * Should be able to create files with normalized words from parsed files.
 *
 * @author Lukáš Kurčík
 * @todo: and more
 */
public interface Preprocessor {

    String NORMALIZED_FILES_DIR = "normalized";
    String NORMALIZED_FILE_EXTENSION = ".txt";
    String CLUSTERING_FILES_DIR = "clustering";
    String VOCAB_FILE = "vocab.txt";
    String CORPUS_FILE = "corpus.dat";

    /**
     * Takes all files from Downloader.PARSED_FILES_DIR, takes words from them, normalizes them and saves them into NORMALIZED_FILES_DIR.
     *
     * @throws IOException when there is problem with file IO
     */
    void normalizeParsedFiles() throws IOException;

    /**
     * Creates two files in CLUSTERING_FILES_DIR.
     * vocab.txt contains all words from all documents. Each word on one line. Line number indicate word ID, lines are numbered from 0.
     * corpus.dat with data about all documents. Each line is in format
     * File formats are described in Corpus and Vocabulary classes
     *
     * @throws IOException when there is problem with file IO
     */
    void prepareClusteringFiles() throws IOException;

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
