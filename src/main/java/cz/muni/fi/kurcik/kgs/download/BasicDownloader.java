package cz.muni.fi.kurcik.kgs.download;

import cz.muni.fi.kurcik.kgs.download.parser.Parser;
import cz.muni.fi.kurcik.kgs.download.parser.ParserException;
import cz.muni.fi.kurcik.kgs.download.parser.ParserMatcher;
import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

    final protected HashSet<URI> parsedUrls = new HashSet<>();
    final protected HashMap<Long, URI> urlsIds = new HashMap<>();
    final protected LinkedList<DownloadURL> queue = new LinkedList<>();

    protected long idCounter = 1;

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
        for (String folder : folders) {
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
     * Each file have its original content saved into original/ID.extension and parsed content as parsed/ID.txt, where ID is assigned by Downloader.
     * URLs linked from site are put into ID.links file, each URL on separate line.
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
            URI normalizeUrl = normalizeUrl(durl.getUrl());
            if (parsedUrls.contains(normalizeUrl))
                continue;
            logger.info("Parsing " + durl.getUrl());

            Parser parser = parserMatcher.getParser(durl.getUrl());

            if (parser == null) {
                logger.info("No parser found");
                continue;
            }

            //@todo: check media language

            Long id = nextId();
            urlsIds.put(id, durl.getUrl());

            logger.info("Downloading " + durl.getUrl());
            String fileName = id + "." + parser.getExtension();
            if (!parser.saveContent(downloadDir
                    .resolve(ORIGINAL_FILES_DIR)
                    .resolve(fileName))) {
                logger.warning("Couldn't save " + durl.getUrl());
            }

            try {
                if (!parser.saveParsed(downloadDir.resolve(PARSED_FILES_DIR).resolve(id + PARSED_EXTENSION))) {
                    logger.warning("Couldn't save parsed " + durl.getUrl());
                }
            } catch (ParserException e) {
                logger.warning("Couldn't parse " + durl.getUrl());
                continue;
            }

            Set<URI> links = parser.getLinks();
            saveUrls(downloadDir.resolve(id + LINKS_EXTENSION), links);
            addNewURLs(durl, links, depth, hops);

            parsedUrls.add(normalizeUrl);
            logger.info("Finished " + durl.getUrl());
            break;
        }

        logger.info("Saving ID -> URL pairs");
        saveIdPairs();
        logger.info("Ended parsing domain " + url);

        removeLoggerFileHandler();
    }

    /**
     * Removes fragment from URI
     * @param uri
     * @return uri without fragment
     */
    protected URI normalizeUrl(URI uri) {
        if (uri.getFragment() == null)
            return uri;
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery());
        } catch (URISyntaxException e) {
            logger.warning("Couldn't normalize url " + uri.toString() + ": " + e.getMessage());
        }
        return uri;
    }

    /**
     * Add new URLs into queue if they are supposed to be added according to download parameters.
     * Depth can't exceed maxDepth and number of hops can't exceed maxHops.
     *
     * @param parent   Parent url
     * @param links    Set of new urls to be added into queue
     * @param maxDepth Maximum depth from root URL
     * @param maxHops  Maximum number of hops from root URL
     */
    protected void addNewURLs(DownloadURL parent, Set<URI> links, int maxDepth, int maxHops) {
        if (parent.getDepth() == maxDepth)
            return;
        for (URI url : links) {
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
     * Save set of urls into file. Each URL on separet line.
     * @param file File to save into
     * @param urls Set of urls
     */
    protected void saveUrls(Path file, Set<URI> urls) {
        try {
            Files.write(file, urls.stream().map(URI::toString).collect(Collectors.toList()), Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.severe("Couldn't save links into " + file.toString() + ": " + e.getMessage());
        }
    }

    /**
     * Return next ID for URL, starts from 0
     * @return next ID
     */
    protected long nextId() {
        return idCounter++;
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
