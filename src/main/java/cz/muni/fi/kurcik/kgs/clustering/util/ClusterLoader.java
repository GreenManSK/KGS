package cz.muni.fi.kurcik.kgs.clustering.util;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static cz.muni.fi.kurcik.kgs.clustering.Clustering.CLUSTERING_FILE;

/**
 * Helper for loading cluster-document pairs
 * Documents are numbered from 0
 *
 * @author Lukáš Kurčík
 */
public class ClusterLoader {
    private final Logger logger;
    protected Path clusteringFile;

    protected Map<Integer, List<Integer>> clusterToDoc;
    protected List<Integer> docToCluster;
    protected int clusters;

    /**
     * Creates link mapper
     *
     * @param clusteringFile
     * @param logger
     */
    public ClusterLoader(Path clusteringFile, Logger logger) {
        this.logger = logger;
        this.clusteringFile = clusteringFile;
    }

    /**
     * Loads information about document clustering
     *
     * @throws IOException when there is problem with file IO
     */
    protected void loadClusters() throws IOException {
        if (docToCluster != null && clusterToDoc != null)
            return;
        logger.info("Loading clustering");
        List<String> lines = FileUtils.readLines(clusteringFile.toFile(), Charsets.UTF_8);
        clusters = lines.get(0).split(" ").length;

        docToCluster = new ArrayList<>(lines.size());
        clusterToDoc = new HashMap<>();

        lines.forEach(line -> {
            double[] probs = Arrays.stream(line.split(" ")).filter(s -> !s.isEmpty()).mapToDouble(Double::parseDouble).toArray();
            double max = 0;
            int index = 0;
            for (int i = 0; i < probs.length; ++i)
                if (max < probs[i]) {
                    max = probs[i];
                    index = i;
                }
            docToCluster.add(index);
            if (!clusterToDoc.containsKey(index))
                clusterToDoc.put(index, new ArrayList<>());
            clusterToDoc.get(index).add(docToCluster.size() - 1);
        });
    }

    /**
     * Return pairs of document id -> cluster
     *
     * @return pairs document id -> cluster
     * @throws IOException when there is problem with file IO
     */
    public List<Integer> getDocToCluster() throws IOException {
        loadClusters();
        return docToCluster;
    }

    /**
     * Return pairs of cluster -> list ofdocument ids
     *
     * @return pairs cluster -> list of document ids
     * @throws IOException when there is problem with file IO
     */
    public Map<Integer, List<Integer>> getClusterToDoc() throws IOException {
        loadClusters();
        return clusterToDoc;
    }

    /**
     * Return number of clusters
     *
     * @return number of clusters
     * @throws IOException when there is problem with file IO
     */
    public int getClusters() throws IOException {
        loadClusters();
        return clusters;
    }
}
