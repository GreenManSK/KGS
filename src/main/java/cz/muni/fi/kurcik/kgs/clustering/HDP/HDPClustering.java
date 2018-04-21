package cz.muni.fi.kurcik.kgs.clustering.HDP;

import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import cz.muni.fi.kurcik.kgs.clustering.index.GradedDistanceIndex;
import cz.muni.fi.kurcik.kgs.clustering.util.ClusterSaver;
import cz.muni.fi.kurcik.kgs.util.AModule;
import cz.muni.fi.kurcik.kgs.util.LogOutputStream;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;
import de.uni_leipzig.informatik.asv.utils.CLDACorpus;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;

/**
 * Clustering using Hierarchical Dirichlet Processes
 */
public class HDPClustering extends AModule implements Clustering {

    protected final static String MODEL_FILE = "model.dat";

    /**
     * Create new majka preprocessor
     */
    public HDPClustering() {
    }

    /**
     * Takes prerocessed data from CLUSTERING_FILES_DIR and makes clusters from them.
     * Saves list of topic probabilities for each document into CLUSTERING_FILE and each cluster into separate file
     *
     * @throws IOException when there is problem with file IO
     */
    @Override
    public void cluster() throws IOException {
        getLogger().info("Starting clustering");
        try (FileInputStream fileInputStream = new FileInputStream(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CORPUS_FILE).toFile())) {
            HDPGibbsSampler2 hdp = new HDPGibbsSampler2();

            getLogger().info("Preparing corpus");
            CLDACorpus corpus = new CLDACorpus(fileInputStream);

            getLogger().info("Preparing HDP");
            hdp.addInstances(corpus.getDocuments(), corpus.getVocabularySize());

            getLogger().info("Computing HDP");
            hdp.run(0, 2000, new PrintStream(new LogOutputStream(getLogger(), Level.INFO)));

            getLogger().info("Saving model");
            saveModel(hdp);

            getLogger().info("Saving clusters");
            saveClusters(hdp, corpus);

            getLogger().info("Computing index of clustering");
            computeClusteringIndex();

            getLogger().info("Clustering finished");
        }
    }

    /**
     * Computes and log clustering index
     *
     * @throws IOException when there is problem with file IO
     */
    protected void computeClusteringIndex() throws IOException {
        try {
            HDPClusteringModel model = new HDPClusteringModel(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CLUSTERING_FILE));
            GradedDistanceIndex gdi = new GradedDistanceIndex();
            getLogger().info("GD_index for clustering: " + gdi.compute(model));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while loading clusters", e);
            throw e;
        }
    }

    /**
     * Saves clusters into CLUSTERING_FILE and each cluster into separate file
     *
     * @param hdp    Finished HDP sampler
     * @param corpus Corpus with documents form HDP sampler
     * @throws IOException when there is problem with file IO
     */
    protected void saveClusters(HDPGibbsSampler2 hdp, CLDACorpus corpus) throws IOException {
        try {
            HDPModel model = hdp.getModel();
            int[][] documents = corpus.getDocuments();

            getLogger().info("Saving clustering probabilities");
            ClusterSaver.saveClustering(model, documents, downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CLUSTERING_FILE));

            UrlIndex urlIndex = new UrlIndex(downloadDir.resolve("ids.txt"));

            getLogger().info("Saving clusters into files");
            ClusterSaver.saveClusters(model, documents, downloadDir.resolve(CLUSTERING_FILES_DIR), urlIndex);

            getLogger().info("Saving url and cluster pairs");
            ClusterSaver.saveUrlClusters(model, documents, downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(URL_CLUSTER_FILE), urlIndex);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while saving clusters", e);
            throw e;
        }
    }

    /**
     * Saves HDP model into file
     *
     * @param hdp Finished HDP sampler
     * @throws IOException when there is problem with file IO
     */
    protected void saveModel(HDPGibbsSampler2 hdp) throws IOException {
        try {
            hdp.saveModel(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(MODEL_FILE));
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Error while saving clustering model", e);
            throw e;
        }
    }
}
