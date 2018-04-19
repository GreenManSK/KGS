package cz.muni.fi.kurcik.kgs.linkmining.Mapper;

import com.drew.lang.Charsets;
import cz.muni.fi.kurcik.kgs.download.Downloader;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper for build maps from URL links in documents
 *
 * @author Lukáš Kurčík
 */
public class LinkMapper {
    private final Logger logger;
    protected Path downloadDir;

    protected UrlIndex urlIndex;
    protected Map<Integer, List<String>> links;
    protected Map<Integer, Integer> inLinks;
    protected Map<Integer, Integer> outLinks;
    
    /**
     * Creates link mapper
     * @param downloadDir
     * @param logger
     */
    public LinkMapper(Path downloadDir, Logger logger) {
        this.logger = logger;
        this.downloadDir = downloadDir;
    }

    /**
     * Compute number of outgoing and ingoing links for each url and load list of outgoing links.
     *
     * @throws IOException when there is problem with file IO
     */
    protected void loadLinks() throws IOException {
        if (links != null && inLinks != null && outLinks != null)
            return;
        logger.info("Loading link info");
        links = new HashMap<>();
        inLinks = new HashMap<>();
        outLinks = new HashMap<>();
        getUrlIndex();

        File[] linkFiles = downloadDir.resolve(Downloader.LINKS_FILES_DIR).toFile().listFiles((File dir, String name) -> name.endsWith(Downloader.LINKS_EXTENSION));
        if (linkFiles == null) {
            IOException e = new IOException("Problem while loading clustering");
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        List<String> links;
        for (File file : linkFiles) {
            links = FileUtils.readLines(file, Charsets.UTF_8);
            Integer fileId = Integer.parseInt(FilenameUtils.getBaseName(file.toString()));

            this.links.put(fileId, links);
            outLinks.put(fileId, links.size());
            inLinks.putIfAbsent(fileId, 0);

            for (String link : links) {
                Long id = urlIndex.getId(link);
                if (id == null) //@todo Decrease number of outgoing links when pointing to not processed url?
                    continue;
                Integer inId = id.intValue();
                Integer newIn = inLinks.get(inId);
                newIn = newIn == null ? 1 : newIn + 1;
                inLinks.put(inId, newIn);
            }
        }
    }

    /**
     * Return map of outgoing links for documents
     * @return map of outgoing links for documents
     * @throws IOException  when there is problem with file IO
     */
    public Map<Integer, List<String>> getLinks() throws IOException {
        loadLinks();
        return links;
    }

    /**
     * Return numbers of ingoing links for documents
     * @return Document - number pairs
     * @throws IOException  when there is problem with file IO
     */
    public Map<Integer, Integer> getInLinks() throws IOException {
        loadLinks();
        return inLinks;
    }

    /**
     * Return numbers of outgoing links for documents
     * @return Document - number pairs
     * @throws IOException  when there is problem with file IO
     */
    public Map<Integer, Integer> getOutLinks() throws IOException {
        loadLinks();
        return outLinks;
    }

    /**
     * Loads url index
     *
     * @throws IOException when there is problem with file IO
     */
    public UrlIndex getUrlIndex() throws IOException {
        if (urlIndex != null)
            return urlIndex;
        try {
            logger.info("Loading URL index");
            urlIndex = new UrlIndex(downloadDir.resolve("ids.txt"));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while reading ids from file", e);
            throw e;
        }
        return urlIndex;
    }
}
