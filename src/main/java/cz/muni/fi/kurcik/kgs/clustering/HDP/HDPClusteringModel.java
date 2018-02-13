package cz.muni.fi.kurcik.kgs.clustering.HDP;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.clustering.FuzzyClusteringModel;
import cz.muni.fi.kurcik.kgs.util.MaxIndex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final public class HDPClusteringModel implements FuzzyClusteringModel {

    int clusters;
    int documents;
    double[][] probMatrix;

    /**
     * Creates clustering model based on probabilities
     *
     * @param probabilities Matrix with probabilities, [document][topic]
     */
    public HDPClusteringModel(double[][] probabilities) {
        probMatrix = probabilities;
        documents = probabilities.length;
        if (documents > 0)
            clusters = probabilities[0].length;
        else
            clusters = 0;
    }

    /**
     * Creates distance model based on clustering saved in file
     *
     * @param clustering File with saved clustering form HDPClustering
     * @throws IOException on file IO error
     */
    public HDPClusteringModel(Path clustering) throws IOException {
        List<String> lines = FileUtils.readLines(clustering.toFile(), Charsets.UTF_8);
        if (lines.size() <= 0) {
            clusters = 0;
            documents = 0;
            probMatrix = new double[0][0];
            return;
        }

        documents = lines.size();
        clusters = StringUtils.countMatches(lines.get(0), " ");
        probMatrix = new double[documents][clusters];
        for (int i = 0; i < documents; i++) {
            if (lines.get(i).isEmpty())
                continue;
            probMatrix[i] = Arrays.stream(lines.get(i).split(" ")).mapToDouble(Double::parseDouble).toArray();
        }
    }

    /**
     * Return cluster for document
     *
     * @param documentId document id
     * @return cluster id
     */
    @Override
    public int getCluster(int documentId) {
        return MaxIndex.max(getClusterMatrix(documentId));
    }

    /**
     * Return probability matrix of document
     *
     * @param documentId document id
     * @return array of probabilities for each cluster
     */
    @Override
    public double[] getClusterMatrix(int documentId) {
        return probMatrix[documentId - 1];
    }

    /**
     * Return probability matrix of cluster
     *
     * @param clusterId cluster id
     * @return array of probabilities for each document
     */
    @Override
    public double[] getDocumentMatrix(int clusterId) {
        double[] probs = new double[documents];

        for (int docId = 0; docId < documents; docId++) {
            probs[docId] = probMatrix[docId][clusterId];
        }

        return probs;
    }

    /**
     * Return probability matrix for clustering
     *
     * @return [documentId][clusterId]
     */
    @Override
    public double[][] getProbabilityMatrix() {
        return probMatrix;
    }

    /**
     * Return array of ids of all documents in this cluster
     *
     * @param clusterId cluster id
     * @return array of ids
     */
    @Override
    public int[] getDocuments(int clusterId) {
        ArrayList<Integer> docs = new ArrayList<>();

        for (int docId = 0; docId < documents; ++docId) {
            if (clusterId == getCluster(docId))
                docs.add(docId + 1);
        }

        return docs.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Return number of clusters
     *
     * @return number of clusters
     */
    @Override
    public int getClusterCount() {
        return clusters;
    }

    /**
     * Return number of documents
     *
     * @return number of documents
     */
    @Override
    public int getDocumentCount() {
        return documents;
    }
}
