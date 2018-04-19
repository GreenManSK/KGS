package cz.muni.fi.kurcik.kgs.util;

import java.nio.file.Path;

/**
 * Interface for modules
 * @author Lukáš Kurčík
 */
public interface Module {

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
