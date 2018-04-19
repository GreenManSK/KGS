package cz.muni.fi.kurcik.kgs.linkmining;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import cz.muni.fi.kurcik.kgs.clustering.DistanceModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPClusteringModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPModelBuilder;
import cz.muni.fi.kurcik.kgs.clustering.index.GradedDistanceIndex;
import cz.muni.fi.kurcik.kgs.clustering.util.ClusterSaver;
import cz.muni.fi.kurcik.kgs.download.Downloader;
import cz.muni.fi.kurcik.kgs.linkmining.ranking.Ranking;
import cz.muni.fi.kurcik.kgs.util.MaxIndex;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;
import de.uni_leipzig.informatik.asv.utils.CLDACorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.muni.fi.kurcik.kgs.clustering.Clustering.CLUSTERING_FILE;
import static cz.muni.fi.kurcik.kgs.clustering.Clustering.CLUSTERING_FILES_DIR;

/**
 * Basic link miner based on original link miner from Kiwi
 *
 * @todo: Repair implementation
 * @author Lukáš Kurčík
 */
public class BasicLinkMiner implements LinkMiner {

    private final Logger logger;

    protected Path downloadDir;

    protected UrlIndex urlIndex;

    protected Map<Long, List<String>> links;
    protected Map<Long, Integer> inLinks;
    protected Map<Long, Integer> outLinks;

    protected List<Long> docToCluster;
    protected int clusters;

    /**
     * Map with rankings for all documents and clusters. [documentId][clusterId]
     */
    protected double[][] rankingMap;

    /**
     * Create new link miner
     */
    public BasicLinkMiner() {
        this(Logger.getLogger(BasicLinkMiner.class.getName()));
    }

    /**
     * Create new link miner
     *
     * @param logger Logger for information about processing
     */
    public BasicLinkMiner(Logger logger) {
        this.logger = logger;
    }

    /**
     * Recomputes actual clustering based on number of ingoing and outgoing links.
     * Compares new clustering with old and saves new clustering in the same way as Clustering implementations
     *
     * @param distanceModel Distance model
     * @param ranking       Ranking function used in computing
     * @param strategy      Specify if distances should be compared based on mean or median
     * @throws IOException when there is problem with file IO
     */
    @Override
    public void recompute(DistanceModel distanceModel, Ranking ranking, LinkMiningStrategy strategy) throws IOException {
        logger.info("Starting link mining");
        loadUrlIndex();
        loadLinks();
        loadClusters();

        logger.info("Recalculating clustering based on links");
        rankingMap = new double[docToCluster.size()][clusters];
        List<String> docLinks;

        for (int doc = 0; doc < docToCluster.size(); doc++) {
            int docCluster = docToCluster.get(doc).intValue();

            rankingMap[doc][docCluster] = ranking.clusterRank();
            long in = inLinks.get(doc + 1L);
            long out = outLinks.get(doc + 1L);
            docLinks = links.get(doc + 1L);

            for (String link : docLinks) {
                Long id = urlIndex.getId(link);
                if (id == null)
                    continue;
                int linkId = id.intValue() - 1;

                rankingMap[linkId][docCluster] += ranking.haveLinkRank();
                rankingMap[linkId][docCluster] += ranking.linkRank(in, out);
                rankingMap[linkId][docCluster] += ranking.distanceRank(
                        distanceModel.distanceFromCentroid(docCluster, linkId),
                        strategy == LinkMiningStrategy.MEAN ? distanceModel.meanDistance(docCluster) : distanceModel.medianDistance(docCluster));
            }
        }

        logger.info("Building new HDP model");
        CLDACorpus corpus = getCorpus();
        HDPModel model = createHDPModel(rankingMap, corpus);

        logger.info("Saving clustering after link mining");
        saveClusters(model, corpus);

        logger.info("Computing index of clustering");
        computeClusteringIndex();

        logger.info("Link mining finished");
    }


    /**
     * Computes and log clustering index
     *
     * @throws IOException when there is problem with file IO
     */
    protected void computeClusteringIndex() throws IOException {
        try {
            HDPClusteringModel modelOld = new HDPClusteringModel(downloadDir.resolve(CLUSTERING_FILES_DIR).resolve(CLUSTERING_FILE));
            HDPClusteringModel modelNew = new HDPClusteringModel(downloadDir.resolve(LINKMINING_DIR).resolve(CLUSTERING_FILE));
            GradedDistanceIndex gdi = new GradedDistanceIndex();
            logger.info("GD_index before link mining: " + gdi.compute(modelOld));
            logger.info("GD_index after link mining: " + gdi.compute(modelNew));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while loading clusters", e);
            throw e;
        }
    }

    /**
     * Saves clusters into CLUSTERING_FILE and each cluster into separate file
     *
     * @param model  HDP model
     * @param corpus Corpus with documents
     * @throws IOException when there is problem with file IO
     */
    protected void saveClusters(HDPModel model, CLDACorpus corpus) throws IOException {
        try {
            Files.createDirectories(downloadDir.resolve(LINKMINING_DIR));

            int[][] documents = corpus.getDocuments();
            logger.info("Saving clustering probabilities");
            ClusterSaver.saveClustering(model, documents, downloadDir.resolve(LINKMINING_DIR).resolve(CLUSTERING_FILE));
            logger.info("Saving clusters into files");
            ClusterSaver.saveClusters(model, documents, downloadDir.resolve(LINKMINING_DIR));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while saving clusters", e);
            throw e;
        }
    }

    /**
     * Creates new HDP model based on ranking map
     *
     * @param rankingMap map of ranking for each document-cluster pair
     * @param corpus     corpus of documents
     * @return new HDP Model based on ranking map
     */
    protected HDPModel createHDPModel(double[][] rankingMap, CLDACorpus corpus) {
        HDPModelBuilder builder = new HDPModelBuilder(rankingMap[0].length, corpus.getVocabularySize());

        int[][] documents = corpus.getDocuments();
        for (int docId = 0; docId < rankingMap.length; docId++) {
            int topicId = MaxIndex.max(rankingMap[docId]);
            for (int wordId : documents[docId]) {
                builder.addTopicWordCount(topicId, wordId, 1);
            }
        }

        return builder.build();
    }

    /**
     * Gets corpus data for all documents
     *
     * @return CLDACorpus
     */
    protected CLDACorpus getCorpus() throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(downloadDir.resolve(Clustering.CLUSTERING_FILES_DIR).resolve(Clustering.CORPUS_FILE).toFile())) {
            return new CLDACorpus(fileInputStream);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while getting document corpus", e);
            throw e;
        }
    }

    /**
     * Loads information about document clustering
     *
     * @throws IOException when there is problem with file IO
     */
    protected void loadClusters() throws IOException {
        logger.info("Loading clustering");
        List<String> lines = FileUtils.readLines(downloadDir.resolve(Clustering.CLUSTERING_FILES_DIR).resolve(CLUSTERING_FILE).toFile(), Charsets.UTF_8);
        clusters = lines.get(0).split(" ").length;
        docToCluster = new ArrayList<>(lines.size());
        lines.forEach(line -> {
            double[] probs = Arrays.stream(line.split(" ")).mapToDouble(Double::parseDouble).toArray();
            double max = 0;
            long index = 0;
            for (int i = 0; i < probs.length; ++i)
                if (max < probs[i]) {
                    max = probs[i];
                    index = i;
                }
            docToCluster.add(index);
        });
    }

    /**
     * Compute number of outgoing and ingoing links for each url and load list of outgoing links.
     *
     * @throws IOException when there is problem with file IO
     */
    protected void loadLinks() throws IOException {
        logger.info("Loading link info");
        links = new HashMap<>();
        inLinks = new HashMap<>();
        outLinks = new HashMap<>();

        File[] linkFiles = downloadDir.resolve(Downloader.LINKS_FILES_DIR).toFile().listFiles((File dir, String name) -> name.endsWith(Downloader.LINKS_EXTENSION));
        if (linkFiles == null) {
            IOException e = new IOException("Problem while loading clustering");
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        List<String> links;
        for (File file : linkFiles) {
            links = FileUtils.readLines(file, Charsets.UTF_8);
            Long fileId = Long.parseLong(FilenameUtils.getBaseName(file.toString()));

            this.links.put(fileId, links);
            outLinks.put(fileId, links.size());
            inLinks.putIfAbsent(fileId, 0);

            for (String link : links) {
                Long inId = urlIndex.getId(link);
                Integer newIn = inLinks.get(inId);
                newIn = newIn == null ? 1 : newIn + 1;
                inLinks.put(inId, newIn);
            }
        }
    }

    /**
     * Loads url index
     *
     * @throws IOException when there is problem with file IO
     */
    protected void loadUrlIndex() throws IOException {
        try {
            logger.info("Loading URL index");
            urlIndex = new UrlIndex(downloadDir.resolve("ids.txt"));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while reading ids from file", e);
            throw e;
        }
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

    /**
     * Sets download directory for downloader. All data will be put into dirName/url
     *
     * @param dir Directory to download folder
     */
    @Override
    public void setDownloadDirectory(Path dir) {
        downloadDir = dir;
    }
}
