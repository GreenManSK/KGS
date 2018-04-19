package cz.muni.fi.kurcik.kgs.util;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Abstract functionality of all modules
 * @author Lukáš Kurčík
 */
abstract public class AModule implements Module {

    private Logger logger;
    protected Path downloadDir;

    /**
     * Sets logger for module
     * @param logger Logger
     */
    public void setLogger(Logger logger) {
        if (logger == null) {
            throw new IllegalAccessError("Logger was already set");
        }
        this.logger = logger;
    }

    /**
     * Get logger
     * @return Set logger or default logger if no logger was set
     */
    protected Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(this.getClass().getName());
        }
        return this.logger;
    }

    /**
     * Sets download directory for downloader. All data will be put into dirName/url
     *
     * @param dir Directory to download folder
     */
    @Override
    public void setDownloadDirectory(Path dir) {
        downloadDir = dir;
    }

    /**
     * Returns path to folder with downloaded data
     *
     * @return directory to download folder
     */
    @Override
    public Path getDownloadDirectory() {
        return downloadDir;
    }

}
