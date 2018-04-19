package cz.muni.fi.kurcik.kgs.linkmining;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import cz.muni.fi.kurcik.kgs.clustering.DistanceModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPClusteringModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPModelBuilder;
import cz.muni.fi.kurcik.kgs.clustering.index.GradedDistanceIndex;
import cz.muni.fi.kurcik.kgs.clustering.util.ClusterLoader;
import cz.muni.fi.kurcik.kgs.clustering.util.ClusterSaver;
import cz.muni.fi.kurcik.kgs.download.Downloader;
import cz.muni.fi.kurcik.kgs.linkmining.Mapper.LinkMapper;
import cz.muni.fi.kurcik.kgs.linkmining.ranking.Ranking;
import cz.muni.fi.kurcik.kgs.util.AModule;
import cz.muni.fi.kurcik.kgs.util.MaxIndex;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;
import de.uni_leipzig.informatik.asv.utils.CLDACorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

import static cz.muni.fi.kurcik.kgs.clustering.Clustering.CLUSTERING_FILE;
import static cz.muni.fi.kurcik.kgs.clustering.Clustering.CLUSTERING_FILES_DIR;

/**
 * Basic link miner based on original link miner from Kiwi
 *
 * @todo: Repair implementation
 * @author Lukáš Kurčík
 */
public class BasicLinkMiner extends AModule implements LinkMiner {

    protected UrlIndex urlIndex;

    protected Map<Integer, List<String>> links;
    protected Map<Integer, Integer> inLinks;
    protected Map<Integer, Integer> outLinks;

    protected List<Integer> docToCluster;
    protected int clusters;

    /**
     * Map with rankings for all documents and clusters. [documentId][clusterId]
     */
    protected double[][] rankingMap;

    /**
     * Create new link miner
     */
    public BasicLinkMiner() {
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
        getLogger().info("Starting link mining");

        loadLinkInfo();
        loadClusterInfo();

        getLogger().info("Recalculating clustering based on links");
        rankingMap = new double[docToCluster.size()][clusters];
        List<String> docLinks;

        for (int doc = 0; doc < docToCluster.size(); doc++) {
            int docCluster = docToCluster.get(doc);

            rankingMap[doc][docCluster] = ranking.clusterRank();
            long in = inLinks.get(doc + 1);
            long out = outLinks.get(doc + 1);
            docLinks = links.get(doc + 1);

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

        getLogger().info("Building new HDP model");
        CLDACorpus corpus = getCorpus();
        HDPModel model = createHDPModel(rankingMap, corpus);

        getLogger().info("Saving clustering after link mining");
        saveClusters(model, corpus);

        getLogger().info("Computing index of clustering");
        computeClusteringIndex();

        getLogger().info("Link mining finished");
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
            getLogger().info("GD_index before link mining: " + gdi.compute(modelOld));
            getLogger().info("GD_index after link mining: " + gdi.compute(modelNew));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while loading clusters", e);
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
            getLogger().info("Saving clustering probabilities");
            ClusterSaver.saveClustering(model, documents, downloadDir.resolve(LINKMINING_DIR).resolve(CLUSTERING_FILE));
            getLogger().info("Saving clusters into files");
            ClusterSaver.saveClusters(model, documents, downloadDir.resolve(LINKMINING_DIR));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while saving clusters", e);
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
            getLogger().log(Level.SEVERE, "Error while getting document corpus", e);
            throw e;
        }
    }

    /**
     * Loads information about links
     * @throws IOException when there is problem with file IO
     */
    protected void loadLinkInfo() throws IOException {
        LinkMapper linkMapper = new LinkMapper(downloadDir, getLogger());

        inLinks = linkMapper.getInLinks();
        outLinks = linkMapper.getOutLinks();
        links = linkMapper.getLinks();
        urlIndex = linkMapper.getUrlIndex();
    }

    /**
     * Load clustering info
     * @throws IOException when there is problem with file IO
     */
    protected void loadClusterInfo() throws IOException {
        ClusterLoader clusterLoader = new ClusterLoader(downloadDir, getLogger());
        clusters = clusterLoader.getClusters();
        docToCluster = clusterLoader.getDocToCluster();
    }
}
