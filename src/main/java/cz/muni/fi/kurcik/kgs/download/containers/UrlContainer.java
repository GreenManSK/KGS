package cz.muni.fi.kurcik.kgs.download.containers;

import cz.muni.fi.kurcik.kgs.download.DownloadURL;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Container for helping Downloaders to work with URLs
 * Default depth and number of hops should be 0 and 0
 */
public interface UrlContainer {

    /**
     * Sets maximum depth for queued urls
     * @param dept Maximum depth
     */
    void setDepth(int dept);

    /**
     * Sets maximum number of hops for urls in queue
     * @param hops Maximum number of hops
     */
    void setHops(int hops);

    /**
     * Return HashSet of parsed URLs
     *
     * @return HashSet
     */
    Set<URI> getParsedUrls();

    /**
     * Check if URL was already parsed
     *
     * @param url URL
     * @return true if url was already parsed
     */
    boolean isParsed(URI url);

    /**
     * Set URL as parsed. Should change value for getNextId()
     *
     * @param url
     */
    void setAsParsed(URI url);

    /**
     * Set URL as rejected.
     * @param url
     */
    void setAsRejected(URI url);

    /**
     * Return id that would be assigned to the URL in next setAsParsed() call
     *
     * @return Next id
     */
    long getNextId();

    /**
     * Return Map with all pairs for URL and their IDs
     *
     * @return HashMap
     */
    Map<Long, URI> getIdUrlPairs();


    /**
     * Adds new url to queue if this URL wasn't already parsed
     *
     * @param url   Url
     * @param depth Actual depth for this URL
     * @param hops  Actual number of hops for this URL
     */
    void push(URI url, int depth, int hops);

    /**
     * Push mre URL into queue. Depth and hops are computed based on parent.
     * @param parent Parent url
     * @param url New url
     */
    void push(DownloadURL parent, URI url);

    /**
     * Push set of URLs into queue. Depth and hops are computed based on parent.
     * @param parent Parent url
     * @param list Set of new urls
     */
    void push(DownloadURL parent, Set<URI> list);

    /**
     * Chcek if queue is empty
     * @return true if empty
     */
    boolean isEmpty();

    /**
     * Return new URL that should be parsed
     *
     * @return DownloadURL object for parsing
     */
    DownloadURL pop();
}
