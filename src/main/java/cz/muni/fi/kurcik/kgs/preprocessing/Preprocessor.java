package cz.muni.fi.kurcik.kgs.preprocessing;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for preprocessing of downloaded data
 *
 * Should be able to create files with normalized words from parsed files.
 * @todo: and more
 * @author Lukáš Kurčík
 */
public interface Preprocessor {

    String NORMALIZED_FILES_DIR = "normalized";
    String NORMALIZED_FILE_EXTENSION = ".txt";

    /**
     * Takes all files from Downloader.PARSED_FILES_DIR, takes words from them, normalizes them and saves them into NORMALIZED_FILES_DIR.
     * @throws IOException when there is problem with file IO
     */
    void normalizeParsedFiles() throws IOException;

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
