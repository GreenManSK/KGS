package cz.muni.fi.kurcik.kgs.download;

import java.net.URI;

/**
 * Object with information about downloaded URL
 * @author Lukáš Kurčík
 */
public class DownloadURL {
    private URI url;
    private int depth;
    private int hops;

    /**
     * Creates new DownloadURL
     * @param url url
     * @param hops number of domain hops from root url
     * @param depth distance from root url
     */
    public DownloadURL(URI url, int hops, int depth) {
        this.url = url;
        this.depth = depth;
        this.hops = hops;
    }

    /**
     * Get download url
     * @return url
     */
    public URI getUrl() {
        return url;
    }

    /**
     * Get distance from root url
     * @return distance from root url
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get number of domain hops from root url
     * @return number of domain hops from root url
     */
    public int getHops() {
        return hops;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadURL that = (DownloadURL) o;

        return url != null ? url.equals(that.url) : that.url == null;
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
}
