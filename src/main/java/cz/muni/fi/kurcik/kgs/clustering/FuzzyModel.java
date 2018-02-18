package cz.muni.fi.kurcik.kgs.clustering;

/**
 * Model representation used to classify documents
 *
 * @author Lukáš Kurčík
 */
public interface FuzzyModel {
    /**
     * Classify document based on all words in it
     *
     * @param wordIds array of word ids
     * @return vector of probabilities for each topic
     */
    double[] classifyDoc(int[] wordIds);


    int getTopics();

    int getWords();
}
