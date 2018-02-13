package cz.muni.fi.kurcik.kgs.clustering.util;

import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPModel;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    static public void saveClustering(HDPModel model, int[][] documents, Path file) throws IOException {
        try (PrintStream stream = new PrintStream(file.toFile())) {
            for (int docId = 0; docId < documents.length; docId++) {
                int[] document = documents[docId];
                double[] vector = model.clasifyDoc(document);
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
    static public void saveClusters(HDPModel model, int[][] documents, Path dir) throws IOException {
        Map<Integer, Set<Long>> topicDocs = new HashMap<>();
        for (int i = 0; i < model.getTopics(); i++)
            topicDocs.put(i, new HashSet<>());

        for (int docId = 0; docId < documents.length; docId++) {
            int[] document = documents[docId];
            double[] vector = model.clasifyDoc(document);
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

        for (Map.Entry<Integer, Set<Long>> entry : topicDocs.entrySet()) {
            Integer topic = entry.getKey();
            Set<Long> docs = entry.getValue();
            FileUtils.writeLines(dir.resolve(topic + ".txt").toFile(), docs);
        }
    }
}
