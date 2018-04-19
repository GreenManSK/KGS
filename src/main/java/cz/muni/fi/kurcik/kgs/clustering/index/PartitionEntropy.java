package cz.muni.fi.kurcik.kgs.clustering.index;

import cz.muni.fi.kurcik.kgs.clustering.FuzzyClusteringModel;

import java.util.Arrays;

public class PartitionEntropy implements FuzzyIndex {
    /**
     * Computes index for model
     *
     * @param model fuzzy clustering model
     */
    @Override
    public double compute(FuzzyClusteringModel model) {
        double sum = 0;
        for (double[] probs : model.getProbabilityMatrix()) {
            double uij = Arrays.stream(probs).max().getAsDouble();
            sum += uij * uij;
        }
        return 1.0 / model.getDocumentCount() * sum;
    }
}
