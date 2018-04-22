package cz.muni.fi.kurcik.kgs.download;

import cz.muni.fi.kurcik.kgs.download.containers.UrlContainer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UrlContainer that ignores #hash part of URLs
 */
public class BasicUrlContainer implements UrlContainer {
    private final Logger logger;

    protected int maxDepth = 0;
    protected int maxHops = 0;

    final protected HashSet<URI> parsedUrls = new HashSet<>();
    final protected HashMap<Long, URI> urlsIds = new HashMap<>();
    final protected PriorityQueue<DownloadURL> queue = new PriorityQueue<>();

    protected long idCounter = 1;

    public BasicUrlContainer() {
        logger = Logger.getLogger(BasicUrlContainer.class.getName());
    }

    public BasicUrlContainer(Logger logger) {
        this.logger = logger;
    }

    /**
     * Sets maximum depth for queued urls
     *
     * @param dept Maximum depth
     */
    @Override
    public void setDepth(int dept) {
        maxDepth = dept;
    }

    /**
     * Sets maximum number of hops for urls in queue
     *
     * @param hops Maximum number of hops
     */
    @Override
    public void setHops(int hops) {
        maxHops = hops;
    }

    /**
     * Return HashSet of parsed URLs
     *
     * @return HashSet
     */
    @Override
    public Set<URI> getParsedUrls() {
        return Collections.unmodifiableSet(parsedUrls);
    }

    /**
     * Check if URL was already parsed
     *
     * @param url URL
     * @return true if url was already parsed
     */
    @Override
    public boolean isParsed(URI url) {
        return parsedUrls.contains(normalizeUrl(url));
    }

    /**
     * Set URL as parsed. Should change value for getNextId()
     *
     * @param url
     */
    @Override
    public void setAsParsed(URI url) {
        URI normalized = normalizeUrl(url);
        if (normalized == null)
            normalized = url;
        parsedUrls.add(normalized);
        urlsIds.put(getNextId(), normalized);
        idCounter++;
    }

    /**
     * Return id that would be assigned to the URL in next setAsParsed() call
     *
     * @return Next id
     */
    @Override
    public long getNextId() {
        return idCounter;
    }

    /**
     * Return Map with all pairs for URL and their IDs
     *
     * @return HashMap
     */
    @Override
    public Map<Long, URI> getIdUrlPairs() {
        return Collections.unmodifiableMap(urlsIds);
    }

    /**
     * Adds new url to queue if this URL wasn't already parsed
     *
     * @param url   Url
     * @param depth Actual depth for this URL
     * @param hops  Actual number of hops for this URL
     */
    @Override
    public void push(URI url, int depth, int hops) {
        if (isParsed(url))
            return;

        if (depth > maxDepth || hops > maxHops) {
            logger.log(Level.INFO, "Rejected: " + url + "; depth: " + depth + "; hops: " +hops);
            return;
        }
        DownloadURL downloadURL = new DownloadURL(url, hops, depth);
        queue.add(downloadURL);
    }

    /**
     * Push mre URL into queue. Depth and hops are computed based on parent.
     * @param parent Parent url
     * @param url New url
     */
    @Override
    public void push(DownloadURL parent, URI url) {
        int hops = parent.getHops();

        String host = normalizeUrl(parent.getUrl()).getHost().replaceAll("^www.", "");

        if (!normalizeUrl(url).toString().contains(host)) {
            hops++;
        }
        push(url, parent.getDepth() + 1, hops);
    }

    /**
     * Push set of URLs into queue. Depth and hops are computed based on parent.
     * @param parent Parent url
     * @param list Set of new urls
     */
    @Override
    public void push(DownloadURL parent, Set<URI> list) {
        for (URI url: list)
            push(parent, url);
    }

    /**
     * Return new URL that should be parsed
     *
     * @return DownloadURL object for parsing
     */
    @Override
    public DownloadURL pop() {
        DownloadURL pop = queue.poll();
        while (pop != null && isParsed(pop.getUrl())) {
            pop = queue.poll();
        }
        return pop;
    }

    /**
     * Chcek if queue is empty
     *
     * @return true if empty
     */
    @Override
    public boolean isEmpty() {
        while (!queue.isEmpty() && queue.peek() == null) {
            queue.poll();
        }
        return queue.isEmpty();
    }

    /**
     * Removes fragment from URI
     *
     * @param uri
     * @return uri without fragment
     */
    protected URI normalizeUrl(URI uri) {
        try {
            String path;
            if (uri.getPath() != null && uri.getPath().endsWith("/"))
                path = uri.getPath().replaceAll("/$", "");
            else
                path = uri.getPath();
            return new URI("http", uri.getAuthority(), path, uri.getQuery());
        } catch (URISyntaxException e) {
            logger.warning("Couldn't normalize url " + uri + ": " + e.getMessage());
        }
        return uri;
    }
}
