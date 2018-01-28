package cz.muni.fi.kurcik.kgs.clustering.HDP;

import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import cz.muni.fi.kurcik.kgs.util.LogOutputStream;
import de.uni_leipzig.informatik.asv.utils.CLDACorpus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
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
     * Saves list of topic probabilities for each document into CLUSTERING_FILE
     *
     * @throws IOException when there is problem with file IO
     */
    @Override
    public void cluster() throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CORPUS_FILE).toFile())) {
            HDPGibbsSampler2 hdp = new HDPGibbsSampler2();
            CLDACorpus corpus = new CLDACorpus(fileInputStream);
            hdp.addInstances(corpus.getDocuments(), corpus.getVocabularySize());

            hdp.run(0, 2000, new PrintStream(new LogOutputStream(logger, Level.INFO)));
            saveModel(hdp);
            saveClusters(hdp, corpus);
        }
    }

    /**
     * Saves clusters into CLUSTERING_FILE
     *
     * @param hdp    Finished HDP sampler
     * @param corpus Corpus with documents form HDP sampler
     * @throws IOException
     */
    protected void saveClusters(HDPGibbsSampler2 hdp, CLDACorpus corpus) throws IOException {

        try (PrintStream stream = new PrintStream(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CLUSTERING_FILE).toString())) {
            HDPModel model = hdp.getModel();
            for (int[] document : corpus.getDocuments()) {
                double[] vector = model.clasifyDoc(document);
                    for (double prob: vector) {
                        stream.format("%.5f ", prob);
                    }
                stream.println();
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Error while saving clusters", e);
            throw new IOException(e);
        }
    }

    /**
     * Saves HDP model into file
     *
     * @param hdp Finished HDP sampler
     */
    protected void saveModel(HDPGibbsSampler2 hdp) {
        try {
            hdp.saveModel(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(MODEL_FILE));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while saving clustering model", e);
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
