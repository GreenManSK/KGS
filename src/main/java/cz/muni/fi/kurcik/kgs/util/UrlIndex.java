package cz.muni.fi.kurcik.kgs.util;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Structure for finding id of URLs
 *
 * @author Lukáš Kurčík
 */
public class UrlIndex {

    final protected Map<Long, URI> idToUrl = new HashMap<>();
    final protected Map<URI, Long> urlToId = new HashMap<>();

    /**
     * Default constructor
     */
    public UrlIndex() {
    }

    /**
     * Creates URL index from id - url map
     *
     * @param idToUrl ID to URL map
     */
    public UrlIndex(Map<Long, URI> idToUrl) {
        idToUrl.forEach(this::add);
    }

    /**
     * Creates url index from file
     *
     * @param file path to saved url index
     * @throws IOException when there is IO problem
     */
    public UrlIndex(Path file) throws IOException {
        try (Stream<String> stream = Files.lines(file)) {
            stream.forEach(line -> {
                if (line.isEmpty())
                    return;
                String[] parts = line.split(" ");
                add(Long.parseLong(parts[1]), URI.create(parts[0]));
            });
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * Return ID of url or null
     *
     * @param url URL
     * @return id or null
     */
    public Long getId(URI url) {
        url = normalize(url);
        Long id = urlToId.get(url);
        if (id != null) {
            return id;
        }

        try {
            URI lastSlash = null;
            lastSlash = new URI(url.getScheme(), url.getAuthority(), url.getPath().replaceAll("/$", ""), url.getQuery());
            id = urlToId.get(lastSlash);
            if (id != null)
                return id;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return urlToId.get(UriBuilder.fromUri(url).scheme(url.getScheme().equals("http") ? "https" : "http").build());
    }

    /**
     * Return ID of url or null
     *
     * @param url URL
     * @return id or null
     */
    public Long getId(String url) {
        return getId(URI.create(url));
    }

    /**
     * Return URL by ID
     *
     * @param id ID
     * @return url or null
     */
    public URI getUrl(long id) {
        return idToUrl.get(id);
    }

    /**
     * Adds new url
     *
     * @param id  ID
     * @param url URL
     */
    public void add(long id, URI url) {
        url = normalize(url);
        urlToId.put(url, id);
        idToUrl.put(id, url);
    }

    /**
     * Normalize URL for this index
     *
     * @param url
     * @return normalized url
     */
    public static URI normalize(URI url) {
        try {
            String path;
            if (url.getPath() != null && url.getPath().endsWith("/"))
                path = url.getPath().replaceAll("/$", "");
            else
                path = url.getPath();
            return new URI("http", url.getAuthority(), path, url.getQuery(), null);
        } catch (URISyntaxException e) {
            return url;
        }
    }

    /**
     * Saves index into file
     *
     * @param file Path to file
     * @throws IOException when there is IO problem
     */
    public void save(Path file) throws IOException {
        Files.write(file, idToUrl.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).map(e -> e.getValue().toString() + " " + e.getKey().toString()).collect(Collectors.toList()), Charset.forName("UTF-8"));
    }
}
