package cz.muni.fi.kurcik.kgs;

import cz.muni.fi.kurcik.kgs.clustering.Clustering;
import cz.muni.fi.kurcik.kgs.clustering.DistanceModel;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPClustering;
import cz.muni.fi.kurcik.kgs.clustering.HDP.HDPDistanceModel;
import cz.muni.fi.kurcik.kgs.clustering.LDA.LDAClustering;
import cz.muni.fi.kurcik.kgs.clustering.corpus.PruningCorpus;
import cz.muni.fi.kurcik.kgs.clustering.util.LogTopicNumber;
import cz.muni.fi.kurcik.kgs.cmd.OptionsBuilder;
import cz.muni.fi.kurcik.kgs.download.BasicDownloader;
import cz.muni.fi.kurcik.kgs.download.BasicUrlContainer;
import cz.muni.fi.kurcik.kgs.download.parser.TikaParserFactory;
import cz.muni.fi.kurcik.kgs.keywords.TextPageRankKeywordGenerator;
import cz.muni.fi.kurcik.kgs.linkmining.BasicLinkMiner;
import cz.muni.fi.kurcik.kgs.linkmining.LinkMiner;
import cz.muni.fi.kurcik.kgs.linkmining.LinkMiningStrategy;
import cz.muni.fi.kurcik.kgs.linkmining.ranking.SimplifiedPageRank;
import cz.muni.fi.kurcik.kgs.preprocessing.MajkaPreprocessor;
import cz.muni.fi.kurcik.kgs.preprocessing.Preprocessor;
import org.apache.commons.cli.*;
import org.apache.tika.langdetect.OptimaizeLangDetector;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Main class for application
 *
 * @author Lukáš Kurčík
 */
public class Main {
    public static void main(String[] args) throws ParseException, IOException {
        Options options = OptionsBuilder.getOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (help(options, cmd))
            return;

        // Check dir
        if (!cmd.hasOption("dir")) {
            System.err.println("Need to provide dir option");
            return;
        }
        Path dir = Paths.get(cmd.getOptionValue("dir"));
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
        }

        // Creates logger
        Logger logger = Logger.getLogger("kgs");
        if (cmd.hasOption("log")) {
            FileHandler fh = new FileHandler(dir.resolve(cmd.getOptionValue("log")).toString());
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        }

        if (cmd.hasOption("L")) {
            logger.setLevel(Level.ALL);
        }

        download(cmd, dir, logger);
        preprocessing(cmd, dir, logger);
        clustering(cmd, dir, logger);
        linkmining(cmd, dir, logger);
        keywords(cmd, dir, logger);
    }

    /**
     * Check keywords mining options
     *
     * @param cmd
     * @param dir
     * @param logger
     * @throws IOException
     */
    public static void keywords(CommandLine cmd, Path dir, Logger logger) throws IOException {
        if (cmd.hasOption("keywords")) {
            TextPageRankKeywordGenerator keywordGenerator = new TextPageRankKeywordGenerator();
            keywordGenerator.setLogger(logger);
            keywordGenerator.setDownloadDirectory(dir);

            if (cmd.hasOption("skiptr"))
                keywordGenerator.setRunTextRank(false);

            keywordGenerator.generateKeywords(
                    dir.resolve(
                            cmd.hasOption("model") && cmd.getOptionValue("model").toLowerCase().compareTo("lm") == 0 ?
                                    LinkMiner.LINKMINING_DIR : Clustering.CLUSTERING_FILES_DIR
                    ).resolve(Clustering.CLUSTERING_FILE),
                    Integer.valueOf(cmd.getOptionValue("w", "10")));
        }
    }

    /**
     * Check link mining options
     *
     * @param cmd
     * @param dir
     * @param logger
     * @throws IOException
     */
    public static void linkmining(CommandLine cmd, Path dir, Logger logger) throws IOException {
        if (cmd.hasOption("linkmining")) {
            LinkMiner linkMiner = new BasicLinkMiner();
            linkMiner.setDownloadDirectory(dir);
            ((BasicLinkMiner) linkMiner).setLogger(logger);

            DistanceModel distanceModel = new HDPDistanceModel(dir.resolve(Clustering.CLUSTERING_FILES_DIR).resolve(Clustering.CLUSTERING_FILE));
            linkMiner.recompute(distanceModel,
                    new SimplifiedPageRank(),
                    cmd.hasOption("distance") && cmd.getOptionValue("distance").toLowerCase().compareTo("mean") == 0
                            ? LinkMiningStrategy.MEAN : LinkMiningStrategy.MEDIAN);
        }
    }

    /**
     * Check clustering options
     *
     * @param cmd
     * @param dir
     * @param logger
     * @throws IOException
     */
    public static void clustering(CommandLine cmd, Path dir, Logger logger) throws IOException {
        if (cmd.hasOption("clustering")) {
            Clustering clustering;

            if (!cmd.hasOption("method") || cmd.getOptionValue("method").toLowerCase().compareTo("hdp") == 0) {
                clustering = new HDPClustering(
                        Double.valueOf(cmd.getOptionValue("alpha", "1")),
                        Double.valueOf(cmd.getOptionValue("beta", "0.5")),
                        Double.valueOf(cmd.getOptionValue("gamma", "1.5"))
                );
                ((HDPClustering) clustering).setLogger(logger);
            } else {
                clustering = new LDAClustering(
                        new LogTopicNumber(),
                        Double.valueOf(cmd.getOptionValue("alpha", "2")),
                        Double.valueOf(cmd.getOptionValue("beta", "0.5"))
                );
                ((LDAClustering) clustering).setLogger(logger);
            }

            clustering.setDownloadDirectory(dir);
            clustering.cluster();
        }
    }

    /**
     * Check downloader options
     *
     * @param cmd
     * @param dir
     * @param logger
     * @throws IOException
     */
    public static void download(CommandLine cmd, Path dir, Logger logger) throws IOException {
        if (cmd.hasOption("downloader")) {
            TikaParserFactory factory = new TikaParserFactory();
            factory.setContentDetection(true);
            BasicDownloader downloader = new BasicDownloader("cs", factory, new OptimaizeLangDetector());
            downloader.setDownloadDirectory(dir);
            downloader.setLogger(logger);
            downloader.downloadPage(
                    URI.create(cmd.getOptionValue("url")),
                    Integer.valueOf(cmd.getOptionValue("hops", "0")),
                    Integer.valueOf(cmd.getOptionValue("depth", "1")));
        }
    }

    /**
     * Check preprocessing options
     *
     * @param cmd
     * @param dir
     * @param logger
     * @throws IOException
     */
    public static void preprocessing(CommandLine cmd, Path dir, Logger logger) throws IOException {
        if (cmd.hasOption("preprocessing")) {
            Preprocessor preprocessor = new MajkaPreprocessor();
            preprocessor.setDownloadDirectory(dir);
            preprocessor.normalizeParsedFiles();
            preprocessor.prepareClusteringFiles(new PruningCorpus(
                    Double.valueOf(cmd.getOptionValue("pruning", "0")),
                    Double.valueOf(cmd.getOptionValue("redundant", "0.3")),
                    Integer.valueOf(cmd.getOptionValue("vocabulary", "2000")),
                    logger));
        }
    }

    /**
     * Check help commands
     *
     * @param options
     * @param cmd
     */
    public static boolean help(Options options, CommandLine cmd) {
        if (cmd.hasOption("helpm")) {
            Options helpOptions;
            switch (cmd.getOptionValue("helpm")) {
                case "downloader":
                    helpOptions = OptionsBuilder.getDownloaderOptions();
                    break;
                case "preprocessing":
                    helpOptions = OptionsBuilder.getPreprocessingOptions();
                    break;
                case "clustering":
                    helpOptions = OptionsBuilder.getClusteringOptions();
                    break;
                case "linkmining":
                    helpOptions = OptionsBuilder.getLinkminingOptions();
                    break;
                case "keywords":
                    helpOptions = OptionsBuilder.getKeywordsOptions();
                    break;
                default:
                    helpOptions = options;
            }
            OptionsBuilder.addCommons(helpOptions);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("-" + cmd.getOptionValue("helpm"), helpOptions);
            return true;
        } else if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(" ", options);
            return true;
        }
        return false;
    }
}
