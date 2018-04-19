package cz.muni.fi.kurcik.kgs.clustering.LDA;

import com.hankcs.lda.Corpus;
import com.hankcs.lda.LdaGibbsSampler;
import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import cz.muni.fi.kurcik.kgs.clustering.FuzzyClusteringModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPClusteringModel;
import cz.muni.fi.kurcik.kgs.clustering.corpus.Vocabulary;
import cz.muni.fi.kurcik.kgs.clustering.index.FuzzyIndex;
import cz.muni.fi.kurcik.kgs.clustering.index.GradedDistanceIndex;
import cz.muni.fi.kurcik.kgs.clustering.util.ClusterSaver;
import cz.muni.fi.kurcik.kgs.util.AModule;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

/**
 * Clustering using Latent Dirichlet Allocation and Gibbs Sampler
 */
public class LDAClustering extends AModule implements Clustering {

    String LDA_CLUSTERS_DIR = "LDA";
    String MODEL_EXT = ".model";

    protected double alpha, beta;

    /**
     * Create new majka preprocessor
     */
    public LDAClustering() {
        this(2.0, 0.5);
    }

    /**
     * Create new majka preprocessor
     *
     * @param alpha  symmetric prior parameter on document--topic associations
     * @param beta   symmetric prior parameter on topic--term associations
     */
    public LDAClustering(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    /**
     * Takes prerocessed data from CLUSTERING_FILES_DIR and makes clusters from them.
     * Saves list of topic probabilities for each document into file and each cluster into separate file
     *
     * @throws IOException when there is problem with file IO
     */
    @Override
    public void cluster() throws IOException {
        getLogger().info("Starting clustering");
        createFolders();

        getLogger().info("Loading corpus");
        Corpus corpus = loadCorpus();

        TreeMap<Double, Integer> gd_index = new TreeMap<>();
        HashMap<Integer, LdaModel> models = new HashMap<>();

        for (int k = 2; k <= 30; k++) {
            getLogger().info("LDA with K = " + k);
            getLogger().info("Creating LDA Gibbs Sampler K = " + k);
            LdaGibbsSampler ldaGibbsSampler = new LdaGibbsSampler(corpus.getDocument(), corpus.getVocabularySize());

            getLogger().info("Training model K = " + k);
            ldaGibbsSampler.gibbs(k, alpha, beta);

            getLogger().info("Getting model  K = " + k);
            double[][] phi = ldaGibbsSampler.getPhi();


            Map<String, Double>[] topicMap = new Map[phi.length];
            for (int q = 0; q < phi.length; ++q) {
                topicMap[q] = new LinkedHashMap<>();

                HashMap<String, Double> help = new HashMap<>();
                for (int i = 0; i < phi[q].length; i++) {
                    help.put(corpus.getVocabulary().getWord(i), phi[q][i]);
                }

                final int x = q;
                help.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).limit(50).forEach(e -> {
                    topicMap[x].put(e.getKey(), e.getValue());
                });
            }

            LdaModel model = new LdaModel(topicMap);
            models.put(k, model);

            getLogger().info("Saving model K = " + k);
            model.saveModel(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(LDA_CLUSTERS_DIR).resolve(k + MODEL_EXT));

            getLogger().info("Saving clustering for K = " + k);
            Path clusteringFile = downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(LDA_CLUSTERS_DIR).resolve(k + ".txt");
            saveClustering(corpus, model, clusteringFile);

            FuzzyClusteringModel clusteringModel = new HDPClusteringModel(clusteringFile); // LDA and HDP use same output
            FuzzyIndex gdi = new GradedDistanceIndex();
            double res = gdi.compute(clusteringModel);
            gd_index.put(res, k);
            getLogger().info("GD_index for LDA with K=" + k + ": " + res);
        }

        int bestK = gd_index.lastEntry().getValue();
        getLogger().info("Best clustering is K=" + bestK + " with GD_index=" + gd_index.lastKey());

        int[][] documents = corpus.getDocument();
        Vocabulary vocabulary = new Vocabulary(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(VOCAB_FILE));
        int[][] translated = new int[documents.length][vocabulary.size()];

        for (int document = 0; document < documents.length; document++) {
            for (int i = 0; i < documents[document].length; i++) {
                translated[document][Integer.parseInt(corpus.getVocabulary().getWord(documents[document][i]))] = documents[document][i];
            }
        }

        getLogger().info("Saving clustering probabilities");
        ClusterSaver.saveClustering(models.get(bestK), documents, downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CLUSTERING_FILE));
        getLogger().info("Saving clusters into files");
        ClusterSaver.saveClusters(models.get(bestK), documents, downloadDir.resolve(CLUSTERING_FILES_DIR), new UrlIndex(downloadDir.resolve("ids.txt")));
    }

    /**
     * Saves clustering for all documents based on model
     *
     * @param corpus Document corpus
     * @param model  LDA model
     * @param path   Path to file
     * @throws IOException when there is problem with file IO
     */
    protected void saveClustering(Corpus corpus, LdaModel model, Path path) throws IOException {
        int[][] documents = corpus.getDocument();
        Vocabulary vocabulary = new Vocabulary(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(VOCAB_FILE));
        int[][] translated = new int[documents.length][vocabulary.size()];

        for (int document = 0; document < documents.length; document++) {
            for (int i = 0; i < documents[document].length; i++) {
                translated[document][Integer.parseInt(corpus.getVocabulary().getWord(documents[document][i]))] = documents[document][i];
            }
        }

        ClusterSaver.saveClustering(model, documents, path);
    }


    /**
     * Loads corpus for LdaGibbsSampler
     *
     * @return Corpus
     * @throws IOException when there is problem with file IO
     */
    protected Corpus loadCorpus() throws IOException {
        Corpus corpus = new Corpus();
        try (FileReader file = new FileReader(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CORPUS_FILE).toFile()); BufferedReader br = new BufferedReader(file)) {
            for (String line; (line = br.readLine()) != null; ) {
                ArrayList<String> words = new ArrayList<>();
                String[] counts = line.split("[ :]");
                for (int i = 1; i < counts.length; i += 2) {
                    String word = counts[i];
                    int count = Integer.parseInt(counts[i + 1]);
                    words.addAll(Collections.nCopies(count, word));
                }
                corpus.addDocument(words);
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while loading corpus", e);
            throw e;
        }
        return corpus;
    }

    /**
     * Creates folders needed for clustering
     *
     * @throws IOException if there is problem with creating folder
     */
    protected void createFolders() throws IOException {
        try {
            Files.createDirectories(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(LDA_CLUSTERS_DIR));
        } catch (IOException e) {
            getLogger().severe("Couldn't create folder '" + downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(LDA_CLUSTERS_DIR).toAbsolutePath().toString() + "' for downloading");
            throw e;
        }
    }
}