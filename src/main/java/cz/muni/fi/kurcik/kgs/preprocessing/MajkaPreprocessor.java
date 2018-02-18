package cz.muni.fi.kurcik.kgs.preprocessing;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.clustering.corpus.BasicCorpus;
import cz.muni.fi.kurcik.kgs.clustering.corpus.Corpus;
import cz.muni.fi.kurcik.kgs.clustering.corpus.PruningCorpus;
import cz.muni.fi.kurcik.kgs.download.Downloader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.muni.fi.kurcik.kgs.clustering.Clustering.CLUSTERING_FILES_DIR;
import static cz.muni.fi.kurcik.kgs.clustering.Clustering.CORPUS_FILE;
import static cz.muni.fi.kurcik.kgs.clustering.Clustering.VOCAB_FILE;

/**
 * Preprocessor implementation using Majka
 *
 * @author Lukáš Kurčík
 */
public class MajkaPreprocessor implements Preprocessor {

    private static final String MAJKA_RESOURCE_DIR = "majka";
    private static final List<String> MAJKA_RESOURCES = Arrays.asList("majka", "majka.w-lt", "preprocess.sh", "stop_words.sed");
    private static final String MAJKA_SCRIPT = "preprocess.sh";

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
        logger.log(Level.INFO, "Normalizing parsed files");
        createProcessedFolder();

        Path parsedDir = downloadDir.resolve(Downloader.PARSED_FILES_DIR);
        Path processedDir = downloadDir.resolve(NORMALIZED_FILES_DIR);

        Path scriptDir = createTempScript();

        File[] parsedFiles = parsedDir.toFile().listFiles((File dir, String name) -> name.endsWith(Downloader.PARSED_EXTENSION));
        String commandBase = getCommandBase(scriptDir);
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
        FileUtils.deleteDirectory(scriptDir.toFile());
        logger.log(Level.INFO, "Normalization finished");
    }

    /**
     * Creates two files in CLUSTERING_FILES_DIR.
     * vocab.txt contains all words from all documents. Each word on one line. Line number indicate word ID, lines are numbered from 0.
     * corpus.dat with data about all documents. Each line is in format
     * File formats are described in Corpus and Vocabulary classes
     *
     * @throws IOException when there is problem with file IO
     */
    @Override
    public void prepareClusteringFiles(Corpus corpus) throws IOException {
        logger.log(Level.INFO, "Preparing clustering files.");
        createCorpusFolder();

        Path processedDir = downloadDir.resolve(NORMALIZED_FILES_DIR);
        File[] parsedFiles = processedDir.toFile().listFiles((File dir, String name) -> name.endsWith(Downloader.PARSED_EXTENSION));
        Arrays.sort(parsedFiles, Comparator.comparingInt(a -> Integer.parseInt(FilenameUtils.removeExtension(a.getName()))));

        if (corpus instanceof PruningCorpus)
            ((PruningCorpus) corpus).setDocCount(parsedFiles.length);

        for (File f : parsedFiles) {
            String[] words = FileUtils.readLines(f, Charsets.UTF_8).get(0).split("\\s+");
            corpus.addDocument(words);
        }

        corpus.save(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CORPUS_FILE));
        corpus.getVocabulary().save(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(VOCAB_FILE));
        logger.log(Level.INFO, "Finished preparing clustering files.");
    }

    /**
     * Return command base form Majka preprocess script
     * It should be used with path to parsed file and path where preprocessed file should be saved
     *
     * @param scriptDir Directory with all files for using majka
     * @return ./pathToScript
     */
    protected String getCommandBase(Path scriptDir) {
        return "./" + scriptDir.resolve(MAJKA_SCRIPT).toAbsolutePath().toString();
    }

    /**
     * Creates temp dir with files for using majka script. Dir needs to be deleted after using
     *
     * @return Path to file
     * @throws IOException
     */
    protected Path createTempScript() throws IOException {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory(this.downloadDir, null);
            for (String file : MAJKA_RESOURCES) {
                try (InputStream input = MajkaPreprocessor.class.getClassLoader().getResourceAsStream(MAJKA_RESOURCE_DIR + "/" + file)) {
                    Files.copy(input, tempDir.resolve(file));
                }
            }
            return tempDir;
        } catch (IOException e) {
            if (tempDir != null)
                FileUtils.deleteDirectory(tempDir.toFile());
            logger.log(Level.WARNING, "Couldn't create temp directory for preprocessor script", e);
            throw e;
        }
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
     * Creates folder for corpus files
     *
     * @throws IOException if there is problem with creating folder
     */
    protected void createCorpusFolder() throws IOException {
        try {
            Files.createDirectories(downloadDir.resolve(CLUSTERING_FILES_DIR));
        } catch (IOException e) {
            logger.severe("Couldn't create folder '" + downloadDir.resolve(CLUSTERING_FILES_DIR).toAbsolutePath().toString() + "' for downloading");
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
