package cz.muni.fi.kurcik.kgs.clustering.index;

import cz.muni.fi.kurcik.kgs.clustering.FuzzyClusteringModel;

/**
 * Graded Distance Index for Fuzzy clustering from "A New Cluster Validity Index for Fuzzy Clustering"
 * Should be maximized
 *
 * @author Lukáš Kurčík
 */
public class GradedDistanceIndex implements FuzzyIndex {
    /**
     * Computes index for model
     *
     * @param model fuzzy clustering model
     */
    @Override
    public double compute(FuzzyClusteringModel model) {
        double sum = 0;
        for (double[] probs : model.getProbabilityMatrix()) {
            double max1 = 0, max2 = 0;
            for (double n : probs) {
                if (n > max1) {
                    max2 = max1;
                    max1 = n;
                } else if (n > max2) {
                    max2 = n;
                }
            }
            sum += max1 - max2;
        }
        return (sum - model.getClusterCount()) / model.getDocumentCount();
    }
}
