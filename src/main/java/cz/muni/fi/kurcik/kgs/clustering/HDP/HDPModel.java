package cz.muni.fi.kurcik.kgs.clustering.HDP;


import java.util.List;

/**
 * Model representation used to classify documents
 *
 * @author Lukáš Kurčík
 */
public class HDPModel {
    protected int topics = 0;
    protected int words = 0;
    protected List<int[]> topicToWordCounts;
    protected int[] wordCount;

    /**
     * Constructor used for building model by builder
     *
     * @param topics            number of topics
     * @param words             number of words
     * @param topicToWordCounts matrix with all word count for topics
     * @param wordCount         word counts across all topics
     */
    HDPModel(int topics, int words, List<int[]> topicToWordCounts, int[] wordCount) {
        this.topics = topics;
        this.wordCount = wordCount;
        this.words = words;
        this.topicToWordCounts = topicToWordCounts;
    }

    /**
     * Classify document based on all words in it
     *
     * @param wordIds array of word ids
     * @return vector of probabilities for each topic
     */
    public double[] clasifyDoc(int[] wordIds) {
        double[] vector = new double[topics];
        for (int topic = 0; topic < topics; ++topic) {
            double probSum = 0;
            int[] topicBag = topicToWordCounts.get(topic);
            for (int wordId : wordIds) {
                probSum += 1.0 * topicBag[wordId] / this.wordCount[wordId];
            }
            vector[topic] = probSum / wordIds.length;
        }
        return vector;
    }

    /**
     * Classify document based on all words in it
     *
     * @param wordIds array of word ids
     * @return id of most probable topic
     */
    public int topicForDoc(int[] wordIds) {
        double[] vector = clasifyDoc(wordCount);
        int maxId = 0;
        double max = vector[0];
        for (int i = 0; i < vector.length; ++i) {
            if (vector[i] > max) {
                max = vector[i];
                maxId = i;
            }
        }
        return maxId;
    }

    public int getTopics() {
        return topics;
    }

    public int getWords() {
        return words;
    }
}

