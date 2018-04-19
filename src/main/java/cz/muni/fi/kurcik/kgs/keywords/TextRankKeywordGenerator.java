package cz.muni.fi.kurcik.kgs.keywords;

import com.drew.lang.Charsets;
import com.sharethis.textrank.MetricVector;
import cz.muni.fi.kurcik.kgs.download.Downloader;
import cz.muni.fi.kurcik.kgs.keywords.TextRank.LanguageCzech;
import cz.muni.fi.kurcik.kgs.keywords.TextRank.TextRank;
import cz.muni.fi.kurcik.kgs.util.AModule;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Keyword generator using TextRank
 *
 * @author Lukáš Kurčík
 */
public class TextRankKeywordGenerator extends AModule implements KeywordGenerator {

    protected final int MAX_NGRAM_LENGTH = 3;
    /**
     * Create new TextRank keyword generator
     */
    public TextRankKeywordGenerator() {
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
        List<Set<Integer>> clusters = getClusters(clusteringFile);

        createKeywordsFolder();

        int clusterNumber = 0;
        for (Set<Integer> cluster : clusters) {
            getLogger().info("Generating keywords for cluster " + clusterNumber);
            Path clusterWords = downloadDir.resolve(KEYWORDS_FILES_DIR).resolve(clusterNumber + ".txt");
            Path clusterText = downloadDir.resolve(KEYWORDS_FILES_DIR).resolve(clusterNumber + ".sentences.txt");
            StringBuilder textBuilder = new StringBuilder();

            try (FileWriter fw = new FileWriter(clusterText.toFile()); BufferedWriter bf = new BufferedWriter(fw)) {
                for (Integer docId : cluster) {
                    String content = FileUtils.readFileToString(downloadDir.resolve(Downloader.PARSED_FILES_DIR).resolve(docId + Downloader.PARSED_EXTENSION).toFile(), Charsets.UTF_8);
                    bf.write(content);
                    textBuilder.append(content);
                }

                TextRank tr = new TextRank(new LanguageCzech(true));
                tr.setMaxNgramLength(MAX_NGRAM_LENGTH);
                tr.prepCall(textBuilder.toString(), false);

                final FutureTask<Collection<MetricVector>> task = new FutureTask<>(tr);
                Collection<MetricVector> answer = null;

                final Thread thread = new Thread(task);
                thread.run();

                answer = task.get(15000L, TimeUnit.MILLISECONDS);

                FileUtils.writeLines(clusterWords.toFile(), answer.stream()
                        .filter(mv -> mv.metric > com.sharethis.textrank.TextRank.MIN_NORMALIZED_RANK)
                        .map(it -> it.render() + " " + it.value.text)
                        .collect(Collectors.toList()));
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error while generating keywords for cluster " + clusterNumber, e);
            }

            getLogger().info("Keywords for cluster " + (clusterNumber++) + " generated");
        }

        getLogger().info("Finished keywords generation");
    }

    /**
     * Creates folder for keywords
     *
     * @throws IOException if there is problem with creating folder
     */
    protected void createKeywordsFolder() throws IOException {
        try {
            Files.createDirectories(downloadDir.resolve(KEYWORDS_FILES_DIR));
        } catch (IOException e) {
            getLogger().severe("Couldn't create folder '" + downloadDir.resolve(KEYWORDS_FILES_DIR).toAbsolutePath().toString() + "' for downloading");
            throw e;
        }
    }

    /**
     * Return list with set of url ids for each cluster
     *
     * @param clusteringFile Path to clustering file
     * @return set list
     */
    protected List<Set<Integer>> getClusters(Path clusteringFile) throws IOException {
        List<Set<Integer>> clusters = new ArrayList<>();

        //@todo: Use new clustering formatting
        List<String> lines = FileUtils.readLines(clusteringFile.toFile(), Charsets.UTF_8);
        for (int doc = 1; doc <= lines.size(); doc++) {
            List<Double> probs = Arrays.stream(lines.get(doc - 1).split(" ")).map(Double::parseDouble).collect(Collectors.toList());
            int clus = 0;
            double max = 0;
            for (int i = 0; i < probs.size(); i++) {
                if (probs.get(i) > max) {
                    clus = i;
                    max = probs.get(i);
                }
            }
            while (clusters.size() <= clus) {
                clusters.add(new HashSet<>());
            }
            clusters.get(clus).add(doc);
        }


        return clusters;
    }
}
