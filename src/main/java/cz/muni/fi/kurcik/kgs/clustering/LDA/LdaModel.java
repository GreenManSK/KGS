package cz.muni.fi.kurcik.kgs.clustering.LDA;

import com.hankcs.lda.LdaGibbsSampler;
import cz.muni.fi.kurcik.kgs.clustering.FuzzyModel;
import org.apache.commons.math.special.Gamma;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Model representation used to classify documents
 *
 * @author Lukáš Kurčík
 */
public class LdaModel implements FuzzyModel {

    protected int topics = 0;
    protected int words = 0;
    final protected Map<Integer, Double>[] topicModels;
    double alpha, beta;
    int[] nwsum;
    int [][] nw;

    /**
     * Construct from translated phi matrix from LdaGibbsSampler
     *
     * @param translatedPhi translated phi matrix
     */
    public LdaModel(Map<String, Double>[] translatedPhi) {
        this(translatedPhi, null);
    }

    /**
     * Construct from translated phi matrix from LdaGibbsSampler
     *
     * @param translatedPhi translated phi matrix
     * @param ldaGibbsSampler Gibbs sampler for model
     */
    public LdaModel(Map<String, Double>[] translatedPhi, LdaGibbsSampler ldaGibbsSampler) {
        topics = translatedPhi.length;
        if (topics > 0) {
            topicModels = new Map[topics];
            for (int topic = 0; topic < topics; topic++) {
                topicModels[topic] = translatedPhi[topic].entrySet().stream().collect(Collectors.toMap(e -> Integer.parseInt(e.getKey()), Map.Entry::getValue));
            }
            words = translatedPhi[0].size();
        } else {
            topicModels = new Map[0];
        }
        this.loadSampler(ldaGibbsSampler);
    }

    /**
     * Loads data from lda gibbs sampler
     * @param ldaGibbsSampler Gibbs sampler for model
     */
    protected void loadSampler(LdaGibbsSampler ldaGibbsSampler) {
        if (ldaGibbsSampler == null)
            return;
        List<Integer> usedWords = new ArrayList<>();
        for (Map<Integer, Double> topic : topicModels) {
            usedWords.addAll(topic.keySet());
        }
        Class objCalss = ldaGibbsSampler.getClass();
        try {
            Field alpha = objCalss.getDeclaredField("alpha");
            Field beta = objCalss.getDeclaredField("beta");
            Field nw = objCalss.getDeclaredField("nw");

            alpha.setAccessible(true);
            beta.setAccessible(true);
            nw.setAccessible(true);

            this.alpha = alpha.getDouble(ldaGibbsSampler);
            this.beta = beta.getDouble(ldaGibbsSampler);

            int[][] nwArray = (int[][]) nw.get(ldaGibbsSampler);
            this.nw = new int[topics][usedWords.size()];
            this.nwsum = new int[topics];
            int wordIndex = 0;
            for (Integer word : usedWords) {
                for (int t = 0; t < topics; t++) {
                    this.nw[t][wordIndex] = nwArray[word][t];
                    this.nwsum[t] += nwArray[word][t];
                }
                wordIndex++;
            }
        } catch (NoSuchFieldException|IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Classify document based on all words in it
     *
     * @param wordIds array of word ids
     * @return vector of probabilities for each topic
     */
    public double[] classifyDoc(int[] wordIds) {
        double[] vector = new double[topics];
        for (int topic = 0; topic < topics; ++topic) {
            double probSum = 0;
            Map<Integer, Double> topicMap = topicModels[topic];

            for (int wordId : wordIds) {
                if (topicMap.containsKey(wordId))
                    probSum += topicMap.get(wordId);
            }
            vector[topic] = probSum;
        }

        double sum = DoubleStream.of(vector).sum();
        if (sum != 0) {
            double norm = 1 / sum;

            for (int i = 0; i < vector.length; i++)
                vector[i] *= norm;
        } else {
            for (int i = 0; i < vector.length; i++)
                vector[i] += 1;
        }
        return vector;
    }

    public int getTopics() {
        return topics;
    }

    public int getWords() {
        return words;
    }

    /**
     * Saves model into file
     *
     * @param file Path to model file
     * @throws IOException on problem with saving model
     */
    public void saveModel(Path file) throws IOException {
        try (PrintStream stream = new PrintStream(file.toString())) {
            for (Map<Integer, Double> topic : topicModels) {
                topic.entrySet()
                        .stream()
                        .sorted(Comparator.comparing(Map.Entry::getKey))
                        .forEach(entry -> stream.format("%.5f  ", entry.getValue()));
                stream.println();
            }
        }
    }

    /**
     * Computes log of marginal likelihood using harmonic mean method
     * @return log of marginal likelihood
     */
    public double marginalLogLikelihood() {
        if (nw == null || nwsum == null)
            throw new IllegalArgumentException("Marginal log likelihood can be computed only for models with phi matrix provided.");
        double r = 0;
        int vocabSize = nw[0].length;
        r = getTopics() * (Math.log(Gamma.logGamma(vocabSize * this.beta)) - vocabSize * Gamma.logGamma(this.beta));
        for (int i = 0; i < getTopics(); i++) {
            double p = 0;

            for (int j = 0; j < nw[i].length; j++) {
                r += Gamma.logGamma(nw[i][j] + this.beta);
            }

            p -= Gamma.logGamma(nwsum[i] + vocabSize * this.beta);
            r += p;
        }
        return Double.isNaN(r) ? Double.NEGATIVE_INFINITY : r;
    }
}
