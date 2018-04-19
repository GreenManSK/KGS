package cz.muni.fi.kurcik.kgs;

import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import cz.muni.fi.kurcik.kgs.clustering.DistanceModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPClustering;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPDistanceModel;
import cz.muni.fi.kurcik.kgs.clustering.LDA.LDAClustering;
import cz.muni.fi.kurcik.kgs.clustering.corpus.PruningCorpus;
import cz.muni.fi.kurcik.kgs.download.BasicDownloader;
import cz.muni.fi.kurcik.kgs.download.Downloader;
import cz.muni.fi.kurcik.kgs.download.parser.TikaParserFactory;
import cz.muni.fi.kurcik.kgs.keywords.KeywordGenerator;
import cz.muni.fi.kurcik.kgs.keywords.RakeKeywordGenerator;
import cz.muni.fi.kurcik.kgs.keywords.TextRankKeywordGenerator;
import cz.muni.fi.kurcik.kgs.linkmining.BasicLinkMiner;
import cz.muni.fi.kurcik.kgs.linkmining.LinkMiningStrategy;
import cz.muni.fi.kurcik.kgs.linkmining.ranking.SimplifiedPageRank;
import cz.muni.fi.kurcik.kgs.preprocessing.MajkaPreprocessor;
import cz.muni.fi.kurcik.kgs.preprocessing.Preprocessor;
import cz.muni.fi.kurcik.kgs.util.Majka;
import org.apache.tika.langdetect.OptimaizeLangDetector;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author Lukáš Kurčík
 */
public class Main {
    static Logger logger;

    public static void main(String[] args) throws IOException, URISyntaxException {
        logger = Logger.getLogger("downloadLogger");
        Path path;
        if (System.getProperty("os.name").contains("Windows"))
            path = Paths.get("W:/");
        else
            path = Paths.get("/mnt/w/");

//        path = path.resolve("Baka/kreveta/");
        path = path.resolve("Baka/d1702/");
        Files.createDirectories(path);

        FileHandler fh = new FileHandler(path.resolve("all.log").toString());
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);

//        download(path);
//        preproccess(path);
//        clustering(path);
//        lm(path);
        keywords(path);
    }

    public static void keywords(Path path) throws IOException {
//        KeywordGenerator keywordGenerator = new TextRankKeywordGenerator(logger);
//        KeywordGenerator keywordGenerator = new RakeKeywordGenerator(logger);
//        keywordGenerator.setDownloadDirectory(path);
//        keywordGenerator.generateKeywords(path.resolve("linkmining").resolve("clusters.txt"), 10);
    }

    public static void lm(Path path) throws IOException {
        BasicLinkMiner lm = new BasicLinkMiner();
        lm.setDownloadDirectory(path);
        DistanceModel dm = new HDPDistanceModel(path.resolve(Clustering.CLUSTERING_FILES_DIR).resolve(Clustering.CLUSTERING_FILE));
        lm.recompute(dm, new SimplifiedPageRank(), LinkMiningStrategy.MEAN);
    }

    public static void clustering(Path path) throws IOException {
        logger.log(Level.INFO, "----CLUSTERING----");
//        HDPClustering clustering = new HDPClustering(logger);
        LDAClustering clustering = new LDAClustering(10.0, 2.0);
        clustering.setDownloadDirectory(path);
        clustering.cluster();
    }

    public static void download(Path path) throws IOException, URISyntaxException {
        logger = Logger.getLogger("downloadLogger2");

        FileHandler fh = new FileHandler(path.resolve("download.log").toString());
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);

        logger.log(Level.INFO, "----DOWNLOADING----");
        Downloader downloader = new BasicDownloader(
                "cs",
                new TikaParserFactory(),
                new OptimaizeLangDetector());


        downloader.setDownloadDirectory(path);
//        downloader.downloadPage(new URI("http://localhost/baka/"), 0, 1);
        downloader.downloadPage(new URI("http://kreveta.net/"), 0, 3);
    }

    public static void preproccess(Path path) throws IOException {
        logger.log(Level.INFO, "----PREPROCESSING----");
        Preprocessor preprocessor = new MajkaPreprocessor();
        preprocessor.setDownloadDirectory(path);
        preprocessor.normalizeParsedFiles();
        preprocessor.prepareClusteringFiles(new PruningCorpus(0.3, 2000, logger));
    }
}
