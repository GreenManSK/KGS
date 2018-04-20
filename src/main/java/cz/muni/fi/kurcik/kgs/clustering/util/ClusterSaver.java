package cz.muni.fi.kurcik.kgs.clustering.util;

import cz.muni.fi.kurcik.kgs.clustering.FuzzyModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPModel;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Util class for saving clustering and cluster
 *
 * @author Lukáš Kurčík
 */
public class ClusterSaver {

    /**
     * Saves clustering probabilities into file
     *
     * @param model     Clustering model
     * @param documents matrix with word ids for all documents
     * @param file      Path for saving data
     * @throws IOException when there is problem with file IO
     */
    static public void saveClustering(FuzzyModel model, int[][] documents, Path file) throws IOException {
        try (PrintStream stream = new PrintStream(file.toFile())) {
            for (int docId = 0; docId < documents.length; docId++) {
                int[] document = documents[docId];
                double[] vector = model.classifyDoc(document);
                double max = 0;
                for (int p = 0; p < vector.length; p++) {
                    if (vector[p] > max) {
                        max = vector[p];
                    }
                    stream.format("%.5f ", vector[p]);
                }
                stream.println();
            }
        }
    }

    /**
     * Saves document ids for each cluster into separate file .txt inside directory
     *
     * @param model     Clustering model
     * @param documents matrix with word ids for all documents
     * @param dir       Path for saving data
     * @throws IOException when there is problem with file IO
     */
    static public void saveClusters(FuzzyModel model, int[][] documents, Path dir) throws IOException {
        saveClusters(model, documents, dir, null);
    }

    /**
     * Saves document ids for each cluster into separate file .txt inside directory
     *
     * @param model     Clustering model
     * @param documents matrix with word ids for all documents
     * @param dir       Path for saving data
     * @param urlIndex  Index for saving URL address with doc ids
     * @throws IOException when there is problem with file IO
     */
    static public void saveClusters(FuzzyModel model, int[][] documents, Path dir, UrlIndex urlIndex) throws IOException {
        Map<Integer, List<Long>> topicDocs = new HashMap<>();
        for (int i = 0; i < model.getTopics(); i++)
            topicDocs.put(i, new ArrayList<>());

        for (int docId = 0; docId < documents.length; docId++) {
            int[] document = documents[docId];
            double[] vector = model.classifyDoc(document);
            double max = 0;
            int maxId = 0;
            for (int p = 0; p < vector.length; p++) {
                if (vector[p] > max) {
                    max = vector[p];
                    maxId = p;
                }
            }
            topicDocs.get(maxId).add((long) docId + 1);
        }

        saveClusters(topicDocs, dir, urlIndex);
    }

    /**
     * Saves document ids for each cluster into separate file .txt inside directory
     *
     * @param clusterDocuments Map with list of all document IDs for each cluster
     * @param dir              Path for saving data
     * @param urlIndex         Index for saving URL address with doc ids
     * @throws IOException when there is problem with file IO
     */
    static public void saveClusters(Map<Integer, List<Long>> clusterDocuments, Path dir, UrlIndex urlIndex) throws IOException {
        for (Map.Entry<Integer, List<Long>> entry : clusterDocuments.entrySet()) {
            Integer topic = entry.getKey();
            List<String> docs = entry.getValue().stream()
                    .sorted(Long::compare)
                    .map(it -> it.toString() + (urlIndex == null ? "" : " " + urlIndex.getUrl(it)))
                    .collect(Collectors.toList());
            FileUtils.writeLines(dir.resolve(topic + ".txt").toFile(), docs);
        }
    }
}
