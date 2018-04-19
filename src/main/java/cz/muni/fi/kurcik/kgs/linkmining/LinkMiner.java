package cz.muni.fi.kurcik.kgs.linkmining;

import cz.muni.fi.kurcik.kgs.clustering.DistanceModel;
import cz.muni.fi.kurcik.kgs.linkmining.ranking.Ranking;
import cz.muni.fi.kurcik.kgs.util.Module;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Uses data from downloading and clustering for updating clustering based on link map.
 * Needs files from links directory and ids.txt
 *
 * @author Lukáš Kurčík
 */
public interface LinkMiner extends Module {

    String LINKMINING_DIR = "linkmining";

    /**
     * Recomputes actual clustering based on number of ingoing and outgoing links.
     * Compares new clustering with old and saves new clustering in the same way as Clustering implementations
     * Saves all url-cluster pairs into Clustering::URL_CLUSTER_FILE
     * @todo: Saves all url-cluster pairs into URL_CLUSTER_FILE
     *
     * @param distanceModel Distance model
     * @param ranking Ranking function used in computing
     * @param strategy Specify if distances should be compared based on mean or median
     * @throws IOException when there is problem with file IO
     */
    void recompute(DistanceModel distanceModel, Ranking ranking, LinkMiningStrategy strategy) throws IOException;

}
