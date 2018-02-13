package cz.muni.fi.kurcik.kgs.clustering.index;

import cz.muni.fi.kurcik.kgs.clustering.FuzzyClusteringModel;

/**
 * Index for fuzzy clustering
 */
@FunctionalInterface
public interface FuzzyIndex {
    /**
     * Computes index for model
     * @param model fuzzy clustering model
     */
    double compute(FuzzyClusteringModel model);
}
