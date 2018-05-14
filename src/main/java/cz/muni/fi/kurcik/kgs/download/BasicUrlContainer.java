package cz.muni.fi.kurcik.kgs.download;

import cz.muni.fi.kurcik.kgs.download.containers.UrlContainer;
import cz.muni.fi.kurcik.kgs.util.UrlIndex;

import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UrlContainer that ignores #hash part of URLs
 */
public class BasicUrlContainer implements UrlContainer {
    private final Logger logger;

    final protected Pattern hostPattern = Pattern.compile("(([^.]+\\.)?[a-zA-Z]+)$");

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
        parsedUrls.add(normalized);
        logger.info("URL " + normalized + " gets ID " + getNextId());
        urlsIds.put(getNextId(), normalized);
        idCounter++;
    }

    /**
     * Set URL as rejected.
     *
     * @param url
     */
    @Override
    public void setAsRejected(URI url) {
        URI normalized = normalizeUrl(url);
        parsedUrls.add(normalized);
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
        return urlsIds;
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
            logger.log(Level.INFO, "Rejected: " + url + "; depth: " + depth + "; hops: " + hops);
            return;
        }
        DownloadURL downloadURL = new DownloadURL(url, hops, depth);
        queue.add(downloadURL);
    }

    /**
     * Push mre URL into queue. Depth and hops are computed based on parent.
     *
     * @param parent Parent url
     * @param url    New url
     */
    @Override
    public void push(DownloadURL parent, URI url) {
        int hops = parent.getHops();
        if (!getHost(parent.getUrl()).equals(getHost(url))) {
            hops++;
        }

        push(url, parent.getDepth() + 1, hops);
    }

    /**
     * Push set of URLs into queue. Depth and hops are computed based on parent.
     *
     * @param parent Parent url
     * @param list   Set of new urls
     */
    @Override
    public void push(DownloadURL parent, Set<URI> list) {
        for (URI url : list)
            push(parent, url);
    }

    /**
     * Return new URL that should be parsed
     *
     * @return DownloadURL object for parsing
     */
    @Override
    public DownloadURL pop() {
        if (isEmpty())
            return null;
        return queue.poll();
    }

    /**
     * Chcek if queue is empty
     *
     * @return true if empty
     */
    @Override
    public boolean isEmpty() {
        cleanQueue();
        return queue.isEmpty();
    }

    /**
     * Removes parsed and null URLs from queue
     */
    protected void cleanQueue() {
        while (!queue.isEmpty() && (queue.peek() == null || isParsed(queue.peek().getUrl()))) {
            queue.poll();
        }
    }

    /**
     * Removes fragment from URI
     *
     * @param uri
     * @return uri without fragment
     */
    protected URI normalizeUrl(URI uri) {
        return UrlIndex.normalize(uri);
    }

    /**
     * Return host form the url
     *
     * @param url URL
     * @return host or empty string
     */
    protected String getHost(URI url) {
        Matcher matcher = hostPattern.matcher(normalizeUrl(url).getHost());

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
