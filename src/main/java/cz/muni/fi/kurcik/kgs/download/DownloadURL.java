package cz.muni.fi.kurcik.kgs.download;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * Object with information about downloaded URL
 * @author Lukáš Kurčík
 */
public class DownloadURL implements Comparable<DownloadURL> {
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

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * <p>
     * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)
     * <p>
     * <p>The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.
     * <p>
     * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.
     * <p>
     * <p>It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     * <p>
     * <p>In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(@NotNull DownloadURL o) {
        int diff = this.getDepth() - o.getDepth();
        return diff != 0 ? diff : this.getHops() - o.getHops();
    }
}
