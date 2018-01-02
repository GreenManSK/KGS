package cz.muni.fi.kurcik.kgs.download;

import cz.muni.fi.kurcik.kgs.download.parser.Parser;
import cz.muni.fi.kurcik.kgs.download.parser.ParserMatcher;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

/**
 * Basic implementation of downloader
 *
 * @author Lukáš Kurčík
 */
public class BasicDownloader implements Downloader {

    private static final Logger logger = Logger.getLogger(BasicDownloader.class.getName());

    protected boolean logToFile;

    protected Path downloadDir;
    protected ParserMatcher parserMatcher;

    protected HashSet<URI> parsedUrls = new HashSet<>();
    protected HashMap<Integer, URI> urlsIds = new HashMap<>();
    protected LinkedList<DownloadURL> queue = new LinkedList<>();

    protected int idCounter = 1;

    public BasicDownloader() {
        this(false);
    }

    /**
     * Creates new basic downloader.
     *
     * @param logToFile Specify if there should be logs in download folders
     */
    public BasicDownloader(boolean logToFile) {
        this.logToFile = logToFile;
    }

    /**
     * Add file handler to logger so logs can be saved into file
     *
     * @param folder Download folder
     */
    protected void addLoggerFileHandler(Path folder) {
        FileHandler fh;
        try {
            fh = new FileHandler(folder.resolve("download.log").toString());
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.addHandler(fh);
        } catch (IOException expection) {
            logger.warning("Couldn't create log file: " + expection.toString());
        }
    }

    /**
     * Remove file handler from logger
     */
    protected void removeLoggerFileHandler() {
        for (Handler h : logger.getHandlers()) {
            if (h instanceof FileHandler)
                logger.removeHandler(h);
        }
    }

    /**
     * Creates folder for downloading URL and its sub-folders.
     *
     * @throws IOException if there is problem with creating folder
     */
    protected void createDownloadFolder() throws IOException {
        List<String> folders = Arrays.asList("", PARSED_FILES_DIR, ORIGINAL_FILES_DIR);
        for (String folder: folders) {
            try {
                Files.createDirectories(downloadDir.resolve(folder));
            } catch (IOException e) {
                logger.severe("Couldn't create folder '" + downloadDir.resolve(folder).toAbsolutePath().toString() + "' for downloading");
                throw e;
            }
        }
    }

    /**
     * Downloads all files with supported formats from this domain. Data will be put into folder named after domain.
     * Files are downloaded only form specified domain and all domains that are number of specified hops away from this domain.
     * Downloads only files till specified depth, url is depth 0.
     *
     * Each file have its original content saved as ID.extension and parsed content as ID.extension.txt, where ID is assigned by Downloader.
     * ID and URL pairs are saved into ids.txt in format [ID] [URL], each on separate line.
     *
     * @param url   Web page url
     * @param hops  Number of hops to other domains
     * @param depth Depth of file downloading
     * @throws IOException when there is problem with downloading
     */
    @Override
    public void downloadPage(URI url, int hops, int depth) throws IOException {
        createDownloadFolder();

        if (logToFile)
            addLoggerFileHandler(downloadDir);

        logger.info("Started parsing domain " + url);

        queue.add(new DownloadURL(url, 0, 0));
        while (!queue.isEmpty()) {
            DownloadURL durl = queue.pop();
            if (parsedUrls.contains(durl.getUrl()))
                continue;
            logger.info("Parsing " + durl.getUrl());

            Parser parser = parserMatcher.getParser(durl.getUrl());

            if (parser == null) {
                logger.info("No parser found");
                continue;
            }

            urlsIds.put(idCounter++, durl.getUrl());
            addNewURLs(durl, parser.getLinks(), depth, hops);
//            parser.saveContent(downloadDir.);

            parsedUrls.add(durl.getUrl());
            logger.info("Finished " + durl.getUrl());
        }

        logger.info("Saving ID -> URL pairs");
        saveIdPairs();
        logger.info("Ended parsing domain " + url);

        removeLoggerFileHandler();
    }

    /**
     * Add new URLs into queue if they are supposed to be added according to download parameters.
     * Depth can't exceed maxDepth and number of hops can't exceed maxHops.
     *
     * @param parent Parent url
     * @param links Set of new urls to be added into queue
     * @param maxDepth Maximum depth from root URL
     * @param maxHops Maximum number of hops from root URL
     */
    protected void addNewURLs(DownloadURL parent, Set<URI> links, int maxDepth, int maxHops) {
        if (parent.getDepth() == maxDepth)
            return;
        for (URI url: links) {
            int hops = parent.getHops();
            if (!parent.getUrl().getHost().equals(url.getHost())) {
                hops++;
            }
            if (hops > maxHops)
                continue;
            queue.add(new DownloadURL(url, hops, parent.getDepth() + 1));
        }
    }

    /**
     * Saves ID-URL pairs into ids.txt file.
     * Format for each pair is [ID] [URL]
     */
    protected void saveIdPairs() throws IOException {
        try {
        Files.write(downloadDir.resolve("ids.txt"),
                urlsIds.entrySet().stream().map(e -> e.getKey().toString() + " " + e.getValue().toString()).collect(Collectors.toList()),
                Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.severe("Couldn't save ID-URL pairs into file");
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

    /**
     * Set parsers matcher to be used while downloading
     *
     * @param parserMatcher ParserMatcher
     */
    @Override
    public void setParserMatcher(ParserMatcher parserMatcher) {
        this.parserMatcher = parserMatcher;
    }
}
