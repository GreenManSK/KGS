package cz.muni.fi.kurcik.kgs.download;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.download.containers.UrlContainer;
import cz.muni.fi.kurcik.kgs.download.parser.Parser;
import cz.muni.fi.kurcik.kgs.download.parser.ParserException;
import cz.muni.fi.kurcik.kgs.download.parser.ParserFactory;
import cz.muni.fi.kurcik.kgs.download.parser.TikaParser;
import cz.muni.fi.kurcik.kgs.util.AModule;
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
import java.util.stream.Collectors;

/**
 * Basic implementation of downloader
 *
 * @author Lukáš Kurčík
 */
public class BasicDownloader extends AModule implements Downloader {
    protected final String language;
    protected final LanguageDetector languageDetector;
    protected final ParserFactory parserFactory;

    protected UrlContainer urlContainer;

    /**
     * Create new basic downloader
     *
     * @param language         The ISO 639-1 language code for language that should be used for parsing
     * @param languageDetector Language detector
     * @param parserFactory    Factory for parser used for all files
     */
    public BasicDownloader(String language, ParserFactory parserFactory, LanguageDetector languageDetector) throws IOException {
        this.parserFactory = parserFactory;
        this.language = language;
        this.languageDetector = languageDetector;

        this.languageDetector.loadModels();
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
        urlContainer = new BasicUrlContainer(getLogger());

        urlContainer.setDepth(depth);
        urlContainer.setHops(hops);
        createDownloadFolder();

        try {
            getLogger().info("Started parsing domain " + url);
            urlContainer.push(url, 0, 0);
            while (!urlContainer.isEmpty()) {
                parse(urlContainer.pop());
            }
        } finally {
            getLogger().info("Saving ID -> URL pairs");
            saveIdPairs();
            getLogger().info("Finished parsing domain " + url);
        }
    }

    /**
     * Parse download url
     *
     * @param durl URL container
     */
    protected void parse(DownloadURL durl) {
        if (durl == null)
            return;
        URI url = durl.getUrl();
        getLogger().info("Parsing " + url + "; depth: " + durl.getDepth() + "; hops: " + durl.getHops());

        URI newUrl = resolveRedirects(url);
        if (!url.equals(newUrl)) {
            getLogger().info("Redirect from " + url + " to " + newUrl);
            urlContainer.push(newUrl, durl.getDepth(), durl.getHops());
            urlContainer.setAsRejected(url);
            return;
        }

        String extension = FilenameUtils.getExtension(url.getPath()).replaceAll("\\?.*$", "");
        if (extension == null || extension.equals("")) {
            extension = TikaParser.extensionFromMime(getMime(url));
        } else
            extension = "." + extension;


        getLogger().info("Downloading " + url);
        String fileName = urlContainer.getNextId() + extension;
        Path originalFile = downloadDir.resolve(ORIGINAL_FILES_DIR).resolve(fileName);

        try {
            FileUtils.copyURLToFile(url.toURL(), originalFile.toFile(), 30000, 120000);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while downloading " + url, e);
            return;
        }

        Parser parser = parserFactory.createParser(url, originalFile);
        if (!parser.canBeParsed()) {
            getLogger().info("Can' be parsed: " + url);
            if (!FileUtils.deleteQuietly(originalFile.toFile()))
                getLogger().warning("Couldn't delete " + originalFile);
            urlContainer.setAsRejected(url);
            return;
        }

        getLogger().info("Parsing " + url);
        String content;
        try {
            content = parser.getContent();
        } catch (ParserException e) {
            getLogger().log(Level.SEVERE, "Problem while parsing " + url, e);
            return;
        }

        getLogger().info("Language detection " + url);
        LanguageResult result = languageDetector.detectAll(content).get(0);
        if (!result.getLanguage().equals(language)) {
            getLogger().info("Invalid language " + url);
            if (!FileUtils.deleteQuietly(originalFile.toFile()))
                getLogger().warning("Couldn't delete " + originalFile);
            urlContainer.setAsRejected(url);
            return;
        }

        getLogger().info("Saving parsed " + url);
        Path parsedFile = downloadDir.resolve(PARSED_FILES_DIR).resolve(urlContainer.getNextId() + PARSED_EXTENSION);
        try {
            FileUtils.writeStringToFile(parsedFile.toFile(), content, Charsets.UTF_8);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Couldn't save parsed " + url, e);
        }

        getLogger().info("Linking " + url);
        Set<URI> links = parser.getLinks().stream().map(link -> url.resolve("/").resolve(link)).collect(Collectors.toSet());
        urlContainer.push(durl, links);
        saveUrls(downloadDir.resolve(LINKS_FILES_DIR).resolve(urlContainer.getNextId() + LINKS_EXTENSION), links);

        urlContainer.setAsParsed(url);
        getLogger().info("Finished " + url);
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
            getLogger().log(Level.WARNING, url.toString(), e);
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
            getLogger().log(Level.WARNING, url.toString(), e);
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
                getLogger().severe("Couldn't create folder '" + downloadDir.resolve(folder).toAbsolutePath().toString() + "' for downloading");
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
            getLogger().log(Level.SEVERE, "Couldn't save ID-URL pairs into file", e);
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
            getLogger().log(Level.SEVERE, "Couldn't save links into " + file.toString(), e);
        }
    }
}
