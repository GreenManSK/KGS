package cz.muni.fi.kurcik.kgs.clustering.HDP;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.clustering.DistanceModel;
import cz.muni.fi.kurcik.kgs.util.Vector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Distance model for HDP clustering
 * Centroids are represented as documents that have 100% probability to be in this topic.
 * Using Jensen–Shannon distance
 *
 * @author Lukáš Kurčík
 */
public class HDPDistanceModel implements DistanceModel {

    private double[][] distances;
    private double[] means;
    private double[] medians;

    /**
     * Computes distance model based on probabilities
     *
     * @param probabilities Matrix with probabilities, [document][topic]
     */
    public HDPDistanceModel(double[][] probabilities) {
        compute(probabilities);
    }

    /**
     * Computes distance model based on clustering saved in file
     *
     * @param clustering File with saved clustering form HDPClustering
     * @throws IOException on file IO error
     */
    public HDPDistanceModel(Path clustering) throws IOException {
        List<String> lines = FileUtils.readLines(clustering.toFile(), Charsets.UTF_8);
        if (lines.size() <= 0) {
            distances = new double[0][0];
            means = new double[0];
            medians = new double[0];
            return;
        }

        int documents = lines.size();
        int topics = StringUtils.countMatches(lines.get(0), " ");
        double[][] probabilities = new double[documents][topics];
        for (int i = 0; i < documents; i++) {
            if (lines.get(i).isEmpty())
                continue;
            probabilities[i] = Arrays.stream(lines.get(i).split(" ")).mapToDouble(Double::parseDouble).toArray();
        }
        compute(probabilities);
    }

    /**
     * Compute all distances, medians and means for model
     *
     * @param probabilities Matrix with probabilities, [document][topic]
     */
    protected void compute(double[][] probabilities) {
        int documents = probabilities.length;
        int topics = probabilities[0].length;

        distances = new double[topics][documents];
        means = new double[topics];
        medians = new double[topics];

        for (int topic = 0; topic < topics; ++topic) {
            double distanceSum = 0;

            double[] topicArray = new double[topics];
            topicArray[topic] = 1;
            Vector topicVector = Vector.of(topicArray);
            for (int document = 0; document < documents; ++document) {
                distances[topic][document] = Math.sqrt(JSD(topicVector, Vector.of(probabilities[document])));
            }
            means[topic] = distanceSum / documents;
            medians[topic] = getMedian(distances[topic]);
        }
    }

    /**
     * Compute Jensen-Shannon Divergence between vectors
     *
     * @param p topic vector
     * @param q doc vector
     * @return Jensen-Shannon Divergence
     */
    protected double JSD(Vector p, Vector q) {
        Vector m = p.plus(q).times(0.5);

        return (0.5 * (p.join(m, (a, b) -> b != 0 ? a / b : 0).use(it -> it > 0 ? Math.log(it) : 0).times(p).sum()
                + q.join(m, (a, b) -> b != 0 ? a / b : 0).use(it -> it > 0 ? Math.log(it) : 0).times(q).sum())) / Math.log(2);
    }

    /**
     * Get median from array
     *
     * @param array numbers
     * @return median
     */
    protected double getMedian(double[] array) {
        Arrays.sort(array);
        int middle = array.length / 2;
        if (array.length % 2 == 1)
            return array[middle];
        else
            return (array[middle - 1] + array[middle]) / 2;
    }

    /**
     * Return distance of document from centroid of cluster
     *
     * @param clusterId  cluster id
     * @param documentId document id
     * @return distance from cluster centroid
     */
    @Override
    public double centroidDistance(int clusterId, int documentId) {
        return distances[clusterId][documentId];
    }

    /**
     * Return mean distance of all documents in this cluster from it's centroid
     *
     * @param clusterId cluster id
     * @return mean distance of documents from centroid
     */
    @Override
    public double meanDistance(int clusterId) {
        return means[clusterId];
    }

    /**
     * Return median distance of all documents in this cluster from it's centroid
     *
     * @param clusterId cluster id
     * @return median distance of documents from centroid
     */
    @Override
    public double medianDistance(int clusterId) {
        return medians[clusterId];
    }
}
