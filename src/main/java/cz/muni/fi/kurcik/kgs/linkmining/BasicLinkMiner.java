package cz.muni.fi.kurcik.kgs.linkmining;

import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import cz.muni.fi.kurcik.kgs.clustering.DistanceModel;
import cz.muni.fi.kurcik.kgs.clustering.index.GradedDistanceIndex;
import cz.muni.fi.kurcik.kgs.clustering.util.ClusterLoader;
import cz.muni.fi.kurcik.kgs.clustering.util.ClusterSaver;
import cz.muni.fi.kurcik.kgs.linkmining.Mapper.LinkMapper;
import cz.muni.fi.kurcik.kgs.linkmining.ranking.Ranking;
import cz.muni.fi.kurcik.kgs.linkmining.util.LinkMiningModel;
import cz.muni.fi.kurcik.kgs.util.AModule;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cz.muni.fi.kurcik.kgs.clustering.Clustering.CLUSTERING_FILE;

/**
 * Basic link miner based on original link miner from Kiwi
 *
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
     * Saves all url-cluster pairs into Clustering::URL_CLUSTER_FILE
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
        LinkMiningModel model = computeModel(distanceModel, ranking, strategy);

        getLogger().info("Saving clustering after link mining");
        saveClusters(model);
        saveModel(model);

        getLogger().info("Computing index of clustering");
        computeClusteringIndex(model);

        getLogger().info("Link mining finished");
    }

    /**
     * Computes new model base on link mining
     *
     * @param distanceModel Distance model
     * @param ranking       Ranking function used in computing
     * @param strategy      Specify if distances should be compared based on mean or median
     * @return New model
     */
    protected LinkMiningModel computeModel(DistanceModel distanceModel, Ranking ranking, LinkMiningStrategy strategy) {
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
        return new LinkMiningModel(rankingMap);
    }

    /**
     * Computes and log clustering index
     *
     * @param model link mining model
     */
    protected void computeClusteringIndex(LinkMiningModel model) {
        GradedDistanceIndex gdi = new GradedDistanceIndex();
        getLogger().info("GD_index after link mining: " + gdi.compute(model));
    }

    /**
     * Saves clustering into files
     *
     * @param model link mining model
     * @throws IOException when there is problem with file IO
     */
    protected void saveClusters(LinkMiningModel model) throws IOException {
        Map<Integer, List<Long>> clusterDocs = new HashMap<>();
        for (int i = 0; i < clusters; i++) {
            clusterDocs.put(i, IntStream.of(model.getDocuments(i)).boxed().map(Integer::longValue).collect(Collectors.toList()));
        }

        Map<Integer, Integer> docsToCluster = new HashMap<>();
        for (int docId = 0; docId < model.getDocumentCount(); docId++) {
            docsToCluster.put(docId + 1, model.getCluster(docId));
        }
        try {
            Files.createDirectories(downloadDir.resolve(LINKMINING_DIR));
            getLogger().info("Saving clusters into files");
            ClusterSaver.saveClusters(clusterDocs, downloadDir.resolve(LINKMINING_DIR), urlIndex);

            getLogger().info("Saving url - cluster pairs");
            ClusterSaver.saveUrlClusters(docsToCluster, downloadDir.resolve(LINKMINING_DIR).resolve(Clustering.URL_CLUSTER_FILE), urlIndex);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while saving clusters", e);
            throw e;
        }
    }

    /**
     * Save clusters as one file
     * @param model
     * @throws IOException
     */
    protected void saveModel(LinkMiningModel model) throws IOException {
        try {
            double[][] probMatrix = model.getProbabilityMatrix();
            List<String> content = new ArrayList<>();
            for (int c = 0; c < model.getClusterCount(); c++) {
                StringBuilder s = new StringBuilder();
                for (int d = 0; d < model.getDocumentCount(); d++) {
                    s.append(String.format("%.5f  ", probMatrix[d][c]));
                }
                content.add(s.toString());
            }
            FileUtils.writeLines(
                    downloadDir.resolve(LINKMINING_DIR).resolve(CLUSTERING_FILE).toFile(),
                    content
                    );
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while saving " + CLUSTERING_FILE, e);
            throw e;
        }
    }

    /**
     * Loads information about links
     *
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
     *
     * @throws IOException when there is problem with file IO
     */
    protected void loadClusterInfo() throws IOException {
        ClusterLoader clusterLoader = new ClusterLoader(downloadDir, getLogger());
        clusters = clusterLoader.getClusters();
        docToCluster = clusterLoader.getDocToCluster();
    }
}
