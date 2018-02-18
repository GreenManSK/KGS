package cz.muni.fi.kurcik.kgs.clustering.HDP;

import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import cz.muni.fi.kurcik.kgs.clustering.index.GradedDistanceIndex;
import cz.muni.fi.kurcik.kgs.clustering.util.ClusterSaver;
import cz.muni.fi.kurcik.kgs.util.LogOutputStream;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;
import de.uni_leipzig.informatik.asv.utils.CLDACorpus;
import org.apache.commons.io.FileUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clustering using Hierarchical Dirichlet Processes
 */
public class HDPClustering implements Clustering {

    protected final static String MODEL_FILE = "model.dat";
    private final Logger logger;

    protected Path downloadDir;

    /**
     * Create new majka preprocessor
     */
    public HDPClustering() {
        this(Logger.getLogger(HDPClustering.class.getName()));
    }

    /**
     * Create new majka preprocessor
     *
     * @param logger Logger for information about processing
     */
    public HDPClustering(Logger logger) {
        this.logger = logger;
    }

    /**
     * Takes prerocessed data from CLUSTERING_FILES_DIR and makes clusters from them.
     * Saves list of topic probabilities for each document into CLUSTERING_FILE and each cluster into separate file
     *
     * @throws IOException when there is problem with file IO
     */
    @Override
    public void cluster() throws IOException {
        logger.info("Starting clustering");
        try (FileInputStream fileInputStream = new FileInputStream(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CORPUS_FILE).toFile())) {
            HDPGibbsSampler2 hdp = new HDPGibbsSampler2();
            hdp.alpha = 4.0;
            hdp.beta = 2.0;
            logger.info("Preparing corpus");
            CLDACorpus corpus = new CLDACorpus(fileInputStream);

            logger.info("Preparing HDP");
            hdp.addInstances(corpus.getDocuments(), corpus.getVocabularySize());

            logger.info("Computing HDP");
            hdp.run(0, 2000, new PrintStream(new LogOutputStream(logger, Level.INFO)));

            logger.info("Saving model");
            saveModel(hdp);

            logger.info("Saving clusters");
            saveClusters(hdp, corpus);

            logger.info("Computing index of clustering");
            computeClusteringIndex();

            logger.info("Clustering finished");
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
            logger.info("GD_index for clustering: " + gdi.compute(model));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while loading clusters", e);
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
            logger.info("Saving clustering probabilities");
            ClusterSaver.saveClustering(model, documents, downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CLUSTERING_FILE));
            logger.info("Saving clusters into files");
            ClusterSaver.saveClusters(model, documents, downloadDir.resolve(CLUSTERING_FILES_DIR), new UrlIndex(downloadDir.resolve("ids.txt")));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while saving clusters", e);
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
            logger.log(Level.WARNING, "Error while saving clustering model", e);
            throw e;
        }
    }

    /**
     * Sets download directory for downloader. All data will be put into dirName/url
     *
     * @param dir Directory to download folder
     */
    @Override
    public void setDownloadDirectory(Path dir) {
        downloadDir = dir;
    }

    /**
     * Returns path to folder with downloaded data
     *
     * @return directory to download folder
     */
    @Override
    public Path getDownloadDirectory() {
        return downloadDir;
    }
}
