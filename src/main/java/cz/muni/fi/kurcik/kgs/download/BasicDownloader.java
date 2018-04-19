package cz.muni.fi.kurcik.kgs.download;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.download.containers.UrlContainer;
import cz.muni.fi.kurcik.kgs.download.parser.Parser;
import cz.muni.fi.kurcik.kgs.download.parser.ParserException;
import cz.muni.fi.kurcik.kgs.download.parser.ParserFactory;
import cz.muni.fi.kurcik.kgs.download.parser.TikaParser;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Basic implementation of downloader
 *
 * @author Lukáš Kurčík
 */
public class BasicDownloader implements Downloader {
    private final Logger logger;
    protected final String language;
    protected final LanguageDetector languageDetector;
    protected final ParserFactory parserFactory;

    protected Path downloadDir;

    protected UrlContainer urlContainer;

    /**
     * Create new basic downloader
     *
     * @param language         The ISO 639-1 language code for language that should be used for parsing
     * @param languageDetector Language detector
     * @param parserFactory    Factory for parser used for all files
     */
    public BasicDownloader(String language, ParserFactory parserFactory, LanguageDetector languageDetector) throws IOException {
        this(language, parserFactory, languageDetector, Logger.getLogger(BasicDownloader.class.getName()));
    }

    /**
     * Create new basic downloader
     *
     * @param language         The ISO 639-1 language code for language that should be used for parsing
     * @param languageDetector Language detector
     * @param parserFactory    Factory for parser used for all files
     * @param logger           Logger for information about downloading
     */
    public BasicDownloader(String language, ParserFactory parserFactory, LanguageDetector languageDetector, Logger logger) throws IOException {
        this.parserFactory = parserFactory;
        this.language = language;
        this.languageDetector = languageDetector;
        this.logger = logger;

        this.languageDetector.loadModels();
        this.urlContainer = new BasicUrlContainer(logger);
    }

    /**
     * Downloads all files with supported formats from this domain. Data will be put into folder named after domain.
     * Files are downloaded only form specified domain and all domains that are number of specified hops away from this domain.
     * Downloads only files till specified depth, url is depth 0.
     * <p>
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
        urlContainer.setDepth(depth);
        urlContainer.setHops(hops);
        createDownloadFolder();

        logger.info("Started parsing domain " + url);
        urlContainer.push(url, 0, 0);
        while (!urlContainer.isEmpty()) {
            parse(urlContainer.pop());
        }

        logger.info("Saving ID -> URL pairs");
        saveIdPairs();
        logger.info("Finished parsing domain " + url);
    }

    protected void parse(DownloadURL durl) {
        URI url = durl.getUrl();
        logger.info("Parsing " + url + "; depth: " + durl.getDepth() + "; hops: " + durl.getHops());

        URI newUrl = resolveRedirects(url);
        if (!url.equals(newUrl)) {
            logger.info("Redirect from " + url + " to " + newUrl);
            urlContainer.push(newUrl, durl.getDepth(), durl.getHops());
            return;
        }

        String extension = FilenameUtils.getExtension(url.getPath());
        if (extension == null || extension.equals("")) {
            extension = TikaParser.extensionFromMime(getMime(url));
        } else
            extension = "." + extension;


        logger.info("Downloading " + url);
        String fileName = urlContainer.getNextId() + extension;
        Path originalFile = downloadDir.resolve(ORIGINAL_FILES_DIR).resolve(fileName);

        try {
            FileUtils.copyURLToFile(url.toURL(), originalFile.toFile());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while downloading " + url, e);
            return;
        }

        Parser parser = parserFactory.createParser(url, originalFile);
        if (!parser.canBeParsed()) {
            logger.info("Can' be parsed: " + url);
            if (!FileUtils.deleteQuietly(originalFile.toFile()))
                logger.warning("Couldn't delete " + originalFile);
            return;
        }

        logger.info("Parsing " + url);
        String content;
        try {
            content = parser.getContent();
        } catch (ParserException e) {
            logger.log(Level.SEVERE, "Problem while parsing " + url, e);
            return;
        }

        logger.info("Language detection " + url);
        LanguageResult result = languageDetector.detectAll(content).get(0);
        if (!result.getLanguage().equals(language)) {
            logger.info("Invalid language " + url);
            if (!FileUtils.deleteQuietly(originalFile.toFile()))
                logger.warning("Couldn't delete " + originalFile);
            return;
        }

        logger.info("Saving parsed " + url);
        Path parsedFile = downloadDir.resolve(PARSED_FILES_DIR).resolve(urlContainer.getNextId() + PARSED_EXTENSION);
        try {
            FileUtils.writeStringToFile(parsedFile.toFile(), content, Charsets.UTF_8);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't save parsed " + url, e);
        }

        logger.info("Linking " + url);
        Set<URI> links = parser.getLinks().stream().map(link -> url.resolve(link)).collect(Collectors.toSet());
        urlContainer.push(durl, links);
        saveUrls(downloadDir.resolve(LINKS_FILES_DIR).resolve(urlContainer.getNextId() + LINKS_EXTENSION), links);

        urlContainer.setAsParsed(url);
        logger.info("Finished " + url);
    }

    /**
     * Get content type from URL
     *
     * @param url
     * @return content type or null
     */
    protected String getMime(URI url) {
        try {
            String mime = url.toURL().openConnection().getContentType();
            if (mime != null && mime.contains(";"))
                mime = mime.replaceAll(";.*$", "");
            return mime;
        } catch (IOException e) {
            logger.log(Level.WARNING, url.toString(), e);
        }
        return null;
    }


    /**
     * Resolve redirects for URL and returns the final landing page url
     *
     * @param url Original URL
     * @return Original URL or new URL after redirects
     */
    protected URI resolveRedirects(URI url) {
        return resolveRedirects(url, 0, 5);
    }

    /**
     * Resolve redirects for URL and returns the final landing page url
     *
     * @param url Original URL
     * @param num Actual number of redirects
     * @param max Maximum number of redirects to prevent loops
     * @return Original URL or new URL after redirects
     */
    protected URI resolveRedirects(URI url, int num, int max) {
        if (num == max)
            return url;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpsURLConnection.HTTP_SEE_OTHER) {
                String newUrl = connection.getHeaderField("Location");
                return url.resolve(newUrl);
            }
        } catch (IOException | ClassCastException e) {
            logger.log(Level.WARNING, url.toString(), e);
        }
        return url;
    }


    /**
     * Creates folder for downloading URL and its sub-folders.
     *
     * @throws IOException if there is problem with creating folder
     */
    protected void createDownloadFolder() throws IOException {
        List<String> folders = Arrays.asList("", PARSED_FILES_DIR, ORIGINAL_FILES_DIR, LINKS_FILES_DIR);
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
     * Saves ID-URL pairs into ids.txt file.
     * Format for each pair is [ID] [URL]
     */
    protected void saveIdPairs() throws IOException {
        UrlIndex urlIndex = new UrlIndex(urlContainer.getIdUrlPairs());
        try {
            urlIndex.save(downloadDir.resolve("ids.txt"));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't save ID-URL pairs into file", e);
            throw e;
        }
    }

    /**
     * Save set of urls into file. Each URL on one line.
     *
     * @param file File to save into
     * @param urls Set of urls
     */
    protected void saveUrls(Path file, Set<URI> urls) {
        try {
            Files.write(file, urls.stream().map(URI::toString).collect(Collectors.toList()), Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't save links into " + file.toString(), e);
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
