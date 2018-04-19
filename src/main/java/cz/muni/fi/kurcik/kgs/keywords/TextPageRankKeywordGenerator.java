package cz.muni.fi.kurcik.kgs.keywords;

import com.drew.lang.Charsets;
import com.sharethis.textrank.MetricVector;
import cz.muni.fi.kurcik.kgs.clustering.util.ClusterLoader;
import cz.muni.fi.kurcik.kgs.download.Downloader;
import cz.muni.fi.kurcik.kgs.keywords.TextRank.LanguageCzech;
import cz.muni.fi.kurcik.kgs.keywords.TextRank.TextRank;
import cz.muni.fi.kurcik.kgs.linkmining.Mapper.LinkMapper;
import cz.muni.fi.kurcik.kgs.util.AModule;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;
import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Keyword generator using combination of TextRank and PageRank
 *
 * @author Lukáš Kurčík
 */
public class TextPageRankKeywordGenerator extends AModule implements KeywordGenerator {

    private static final String DOC_KEYWORDS_DIR = "documents";
    private static final double TEXT_RANK_LOWER_BOUND = 0.05D;

    protected int maxNGramLength;

    /**
     * Create new generator
     */
    public TextPageRankKeywordGenerator() {
        this(3);
    }

    /**
     * Create new generator
     *
     * @param maxNGramLength maximum number of words in one keyword
     */
    public TextPageRankKeywordGenerator(int maxNGramLength) {
        this.maxNGramLength = maxNGramLength;
    }

    /**
     * Generates keywords for each cluster and saves them into KEYWORDS_FILES_DIR, each cluster will have separate file.
     * Each keyword on separate line.
     *
     * @param clusteringFile     Path to clustering file
     * @param keywordsForCluster Number of keywords for each cluster to generate
     * @throws IOException when there is problem with file IO
     */
    @Override
    public void generateKeywords(Path clusteringFile, int keywordsForCluster) throws IOException {
        getLogger().info("Starting keywords generation");
        createKeywordsFolder();

        generateDocumentKeywords();
        generateClusterKeywords(keywordsForCluster);
    }

    /**
     * Generate keywords for all clusters
     *
     * @param keywordsForCluster Number of keywords for each cluster to generate
     * @throws IOException when there is problem with file IO
     */
    protected void generateClusterKeywords(int keywordsForCluster) throws IOException {
        getLogger().info("Starting keywords generation for clusters");

        // Load link map
        LinkMapper linkMapper = new LinkMapper(downloadDir, getLogger());
        Map<Integer, List<String>> links = linkMapper.getLinks();
        UrlIndex urlIndex = linkMapper.getUrlIndex();

        // Load clusters
        ClusterLoader clusterLoader = new ClusterLoader(downloadDir, getLogger());
        Map<Integer, List<Integer>> clusterToDoc = clusterLoader.getClusterToDoc();

        for (Map.Entry<Integer, List<Integer>> entry : clusterToDoc.entrySet()) {
            Integer clusterId = entry.getKey();
            List<Integer> documents = entry.getValue().stream().map(v -> v + 1).collect(Collectors.toList());
            getLogger().info("Generating keywords for cluster " + clusterId + "/" + clusterLoader.getClusters());

            Map<Integer, List<String>> docToKeywords = getKeywords(documents);
            Graph<String, DefaultEdge> wordGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

            getLogger().info("Building word graph for cluster " + clusterId);
            for (Map.Entry<Integer, List<String>> keywordsEntry: docToKeywords.entrySet()) {
                Integer document = keywordsEntry.getKey();
                for (String word: keywordsEntry.getValue()) {
                    wordGraph.addVertex(word);
                    for (String link: links.get(document)) {
                        Long id = urlIndex.getId(link);
                        if (id == null || !docToKeywords.containsKey(id.intValue())) {
                            continue;
                        }
                        for (String outWord: docToKeywords.get(id.intValue())) {
                            wordGraph.addVertex(outWord);
                            wordGraph.addEdge(word, outWord);
                        }
                    }
                }
            }

            getLogger().info("Computing page rank for cluster " + clusterId);
            PageRank<String, DefaultEdge> pageRank = new PageRank<>(wordGraph);
            Map<String, Double> scores = pageRank.getScores();

            getLogger().info("Saving keywords for cluster " + clusterId);
            List<String> keywords = scores.entrySet().stream()
                    .sorted(Comparator.comparingDouble(e -> ((Map.Entry<String, Double>) e).getValue()).reversed())
                    .limit(keywordsForCluster)
                    .map(e -> e.getKey() + " " + e.getValue())
                    .collect(Collectors.toList());
            FileUtils.writeLines(
                    downloadDir
                            .resolve(KEYWORDS_FILES_DIR)
                            .resolve(clusterId.toString() + Downloader.PARSED_EXTENSION)
                            .toFile(),
                    keywords);
        }


        getLogger().info("Cluster keywords generated");
    }

    /**
     * Load keywords of provided documents
     *
     * @param documents Document ids
     * @return Map with keyword lists for documents
     * @throws IOException when there is problem with file IO
     */
    protected Map<Integer, List<String>> getKeywords(List<Integer> documents) throws IOException {
        Map<Integer, List<String>> result = new HashMap<>();
        Path keywordDir = downloadDir.resolve(KEYWORDS_FILES_DIR).resolve(DOC_KEYWORDS_DIR);
        for (Integer document: documents) {
            getLogger().info("Loading keywords for document " + document);
            result.put(document,
                    FileUtils.readLines(keywordDir.resolve(document.toString() + Downloader.PARSED_EXTENSION).toFile(), Charsets.UTF_8)
                    .stream().map(e -> e.substring(e.indexOf(' ') + 1))
                            .collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * Generates keywords for all documents
     *
     * @throws IOException when there is problem with file IO
     */
    protected void generateDocumentKeywords() throws IOException {
        getLogger().info("Starting keywords generation for documents");
        File[] parsedDocuments = downloadDir.resolve(Downloader.PARSED_FILES_DIR).toFile()
                .listFiles((File dir, String name) -> name.endsWith(Downloader.PARSED_EXTENSION));
        int finishedDocuments = 0;

        for (File parsed : parsedDocuments) {
            getLogger().info("Document " + parsed.getName() + " "
                    + (finishedDocuments + 1) + "/" + parsedDocuments.length);

            Map<String, Double> keyWords = generateDocumentKeywords(parsed);
            FileUtils.writeLines(downloadDir.resolve(KEYWORDS_FILES_DIR).resolve(DOC_KEYWORDS_DIR).resolve(parsed.getName()).toFile(),
                    keyWords.entrySet().stream().map(e -> e.getValue() + " " + e.getKey()).collect(Collectors.toList()), "\n");

            finishedDocuments++;
        }
        getLogger().info("Document keywords generated");
    }

    /**
     * Generates keywords for document file and saves them
     *
     * @param file document file
     * @return Map of keywords with TextRank values
     */
    protected Map<String, Double> generateDocumentKeywords(File file) throws IOException {
        try {
            final String documentText = FileUtils.readFileToString(file, Charsets.UTF_8);
            final TextRank textRank = prepareTextRank();
            textRank.prepCall(documentText, false);

            Collection<MetricVector> result = textRank.call();
            HashMap<String, Double> words = new HashMap<>();
            for (MetricVector mv : result) {
                if (mv.metric >= TEXT_RANK_LOWER_BOUND) {
                    words.put(mv.value.text, mv.metric);
                }
            }
            return words;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new IOException(e);
        }
    }

    /**
     * Prepare TextRank instance
     *
     * @return TextRank object
     */
    protected TextRank prepareTextRank() throws Exception {
        TextRank textRank;
        textRank = new TextRank(new LanguageCzech(false));
        textRank.setMaxNgramLength(maxNGramLength);
        return textRank;
    }

    /**
     * Creates folder for keywords
     *
     * @throws IOException if there is problem with creating folder
     */
    protected void createKeywordsFolder() throws IOException {
        Path[] dirs = new Path[]{
                downloadDir.resolve(KEYWORDS_FILES_DIR),
                downloadDir.resolve(KEYWORDS_FILES_DIR).resolve(DOC_KEYWORDS_DIR)
        };
        for (Path dir : dirs) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                getLogger().severe("Couldn't create folder '" + dir.toAbsolutePath().toString() + "' for downloading");
                throw e;
            }
        }
    }
}
