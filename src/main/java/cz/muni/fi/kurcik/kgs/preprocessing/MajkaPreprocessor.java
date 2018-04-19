package cz.muni.fi.kurcik.kgs.preprocessing;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.clustering.corpus.Corpus;
import cz.muni.fi.kurcik.kgs.clustering.corpus.PruningCorpus;
import cz.muni.fi.kurcik.kgs.download.Downloader;
import cz.muni.fi.kurcik.kgs.util.Majka;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.muni.fi.kurcik.kgs.clustering.Clustering.*;

/**
 * Preprocessor implementation using Majka
 *
 * @author Lukáš Kurčík
 */
public class MajkaPreprocessor implements Preprocessor {

    protected static final String STOP_WORDS_FILE = "majka/stop_words.txt";

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

        Set<String> stopWords = loadStopWords();
        File[] parsedFiles = parsedDir.toFile().listFiles((File dir, String name) -> name.endsWith(Downloader.PARSED_EXTENSION));
        Majka majka = new Majka();
        for (File parsed : parsedFiles) {
            Path result = processedDir.resolve(parsed.getName());
            String content = FileUtils.readFileToString(parsed, Charsets.UTF_8);
            List<String> tokens = tokenize(content);
            tokens = tokens.stream().filter(token -> !stopWords.contains(token.toLowerCase())).collect(Collectors.toList());

            Map<String, String> lemmas = majka.findAll(tokens, Majka.IGNORE_CASE, false);
            FileUtils.writeLines(result.toFile(), tokens.stream().map(lemmas::get).collect(Collectors.toList()), " ");
        }

        logger.log(Level.INFO, "Normalization finished");
    }

    /**
     * Tokenize string
     *
     * @param content Content of file
     * @return List of word tokens
     */
    protected List<String> tokenize(String content) {
        Properties props = new Properties();
        props.put("annotators", "tokenize");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(content);
        pipeline.annotate(document);
        List<CoreLabel> labels = document.get(CoreAnnotations.TokensAnnotation.class);
        List<String> result = new ArrayList<>();
        for (CoreLabel l : labels) {
            if (!l.toString().matches("^\\W+$")) {
                result.add(l.toString());
            }
        }
        return result;
    }

    /**
     * Load list of stopwords
     *
     * @return HashSet of stopwords
     * @throws IOException when there is problem with file IO
     */
    protected Set<String> loadStopWords() throws IOException {
        Set<String> stopWords = new HashSet<>();
        try (InputStream inputStream = MajkaPreprocessor.class.getClassLoader().getResourceAsStream(STOP_WORDS_FILE);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stopWords.add(line);
            }
        }
        return stopWords;
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
