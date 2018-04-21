package cz.muni.fi.kurcik.kgs.cmd;

import org.apache.commons.cli.Options;

/**
 * Class for building command line options of this application fro apache CLI
 *
 * @author Lukáš Kurčík
 */
public class OptionsBuilder {


    protected static Options options;

    protected OptionsBuilder() {
    }

    /**
     * Return options object
     *
     * @return Options object
     */
    public static Options getOptions() {
        if (options != null)
            return options;
        options = new Options();

        addModules(options);
        addCommons(options);
        addDownloader(options);
        addPreprocessing(options);
        addClustering(options);
        addLinkmining(options);
        addKeywords(options);

        return options;
    }

    public static Options getDownloaderOptions() {
        Options options = new Options();
        addDownloader(options);
        return options;
    }

    public static Options getPreprocessingOptions() {
        Options options = new Options();
        addPreprocessing(options);
        return options;
    }

    public static Options getClusteringOptions() {
        Options options = new Options();
        addClustering(options);
        return options;
    }

    public static Options getLinkminingOptions() {
        Options options = new Options();
        addLinkmining(options);
        return options;
    }

    public static Options getKeywordsOptions() {
        Options options = new Options();
        addKeywords(options);
        return options;
    }

    /**
     * Add options for running modules
     */
    protected static void addModules(Options options) {
        options.addOption("downloader", false, "Run downloader module");
        options.addOption("preprocessing", false, "Run preprocessing module");
        options.addOption("clustering", false, "Run clustering module");
        options.addOption("linkmining", false, "Run link mining module");
        options.addOption("keywords", false, "Run keywords module");
    }

    /**
     * Add options used in all modules
     */
    protected static void addCommons(Options options) {
        options.addOption("h", "help", false,
                "Print this message");
        options.addOption( "helpm", true,
                "Print help for module: downloader, preprocessing, clustering, linkmining, keywords");
        options.addOption("l", "log", true, "Log into file");
        options.addOption("L", "console-log", false, "Write logs into console");
        options.addOption("d", "dir", true, "Directory for all data");
    }

    /**
     * Add options used by downloader
     */
    protected static void addDownloader(Options options) {
        options.addOption("u", "url", true, "Starting url");
        options.addOption("hops", true, "Number of domain hops. Default: 0");
        options.addOption("depth", true, "Maximum depth for downloader. Default: 1");
    }

    /**
     * Add options used by preprocessing
     */
    protected static void addPreprocessing(Options options) {
        options.addOption("v", "vocabulary", true, "Vocabulary size. Default: 2000");
        options.addOption("redundant", true, "Specify percentage of documents, that contains words for word to be dropped. Default: 0.3");
    }

    /**
     * Add options used by clustering
     */
    protected static void addClustering(Options options) {
        options.addOption("c", "method", true, "Clustering method - hdp or lda");
        options.addOption("alpha", true, "Alpha value for model. Default: 2 for LDA, 1 for HDP");
        options.addOption("beta", true, "Beta value for model. Default: 0.5");
        options.addOption("gamma", true, "Gama value for HDP model. Default: 1.5");
    }

    /**
     * Add options used by link mining
     */
    protected static void addLinkmining(Options options) {
        options.addOption("distance", true, "Link mining distance compare method - mean or average");
    }

    /**
     * Add options used by keyword generator
     */
    protected static void addKeywords(Options options) {
        options.addOption("w", "words", true, "Number of keywords for each cluster");
        options.addOption("model", true, "Type of model used for keywords - c for clustering, lm for link mining");
    }
}
