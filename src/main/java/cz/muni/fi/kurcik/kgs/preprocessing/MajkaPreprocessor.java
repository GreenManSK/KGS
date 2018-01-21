package cz.muni.fi.kurcik.kgs.preprocessing;


import cz.muni.fi.kurcik.kgs.download.Downloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Preprocessor implementation using Majka
 *
 * @author Lukáš Kurčík
 */
public class MajkaPreprocessor implements Preprocessor {

    private final Logger logger;

    protected Path downloadDir;

    /**
     * Create new majka preprocessor
     */
    public MajkaPreprocessor() {
        this(Logger.getLogger(MajkaPreprocessor.class.getName()));
    }

    /**
     * Create new majka preprocessor
     *
     * @param logger Logger for information about processing
     */
    public MajkaPreprocessor(Logger logger) {
        this.logger = logger;
    }

    /**
     * Takes all files from Downloader.PARSED_FILES_DIR, takes words from them, normalizes them and saves them into NORMALIZED_FILES_DIR.
     *
     * @throws IOException when there is problem with file IO
     */
    @Override
    public void normalizeParsedFiles() throws IOException {
        createProcessedFolder();

        Path parsedDir = downloadDir.resolve(Downloader.PARSED_FILES_DIR);
        Path processedDir = downloadDir.resolve(NORMALIZED_FILES_DIR);

        File[] parsedFiles = parsedDir.toFile().listFiles((File dir, String name) -> name.endsWith(Downloader.PARSED_EXTENSION));
        String commandBase = getCommandBase();

        for (File parsed : parsedFiles) {
            String cmd =
                    commandBase + " " +
                            parsed.getPath() + " " +
                            processedDir.resolve(parsed.getName()).toString();
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                process.waitFor();
                if (process.exitValue() != 0) {
                    logger.log(Level.WARNING, "Could not process " + parsed);
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Error while processing " + parsed, e);
            }
        }
    }

    /**
     * Return command base form Majka preprocess script
     * It should be used with path to parsed file and path where preprocessed file should be saved
     * @return ./pathToScript
     */
    protected String getCommandBase() {
        return "./" + MajkaPreprocessor.class.getClassLoader().getResource("majka/preprocess.sh").getPath();
    }

    /**
     * Creates folder for processed files
     *
     * @throws IOException if there is problem with creating folder
     */
    protected void createProcessedFolder() throws IOException {
        try {
            Files.createDirectories(downloadDir.resolve(NORMALIZED_FILES_DIR));
        } catch (IOException e) {
            logger.severe("Couldn't create folder '" + downloadDir.resolve(NORMALIZED_FILES_DIR).toAbsolutePath().toString() + "' for downloading");
            throw e;
        }
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
