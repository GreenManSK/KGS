package cz.muni.fi.kurcik.kgs.download.parser;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser form HTML web pages
 * @author Lukáš Kurčík
 */
public class HtmlParser extends AbstractParser {
    private final static Logger logger = Logger.getLogger(HtmlParser.class.getName());

    protected Charset charset;
    protected Document document;

    public HtmlParser(URI url) {
        super(url);
    }

    /**
     * Finds charset defined in html header
     * @return charset if found or UTF8 charset
     */
    protected Charset getCharset() {
        if (charset != null)
            return charset;

        try (InputStream is = new ByteArrayInputStream(getContent())) {
            Document doc = Jsoup.parse(is, StandardCharsets.UTF_8.name(), url.toString());
            Element charsetMeta = doc.select("head meta[charset]").first();
            if (charsetMeta != null) {
                charset = Charset.forName(charsetMeta.attr("charset"));
            } else {
                Element contentTypeMeta = doc.select("head meta[http-equiv=Content-Type]").first();
                if (contentTypeMeta != null) {
                    charset = Charset.forName(getCharsetFromContent(contentTypeMeta.attr("content")));
                } else {
                    logger.warning(url.toString() + " dose not have encoding in header.");
                }
            }
        } catch (IOException e) {
            logger.warning("IO exception for '" + url.toString() + "': " +e.getMessage());
        } catch (UnsupportedCharsetException|IllegalCharsetNameException e) {
            logger.warning("Unsupported charset for " + url.toString() + ": " + e.getMessage());
        }

        if (charset == null)
            charset = StandardCharsets.UTF_8;
        return charset;
    }

    /**
     * Get charset from meta content of Content-Type
     * @param content content attribute
     * @return charset name
     */
    protected String getCharsetFromContent(String content) {
        Matcher m = Pattern.compile(".*charset=([^ ]+).*", Pattern.CASE_INSENSITIVE).matcher(content);
        if (m.matches()) {
            return m.group(1);
        }
        return "";
    }

    /**
     * Return Jsoup document from HTML file
     * @return Document
     */
    protected Document getDocument() {
        if (document != null)
            return document;

        try (InputStream is = new ByteArrayInputStream(getContent())) {
            document = Jsoup.parse(is, getCharset().name(), url.toString());
        } catch (IOException e) {
            logger.warning("IO exception for '" + url.toString() + "': " +e.getMessage());
        }
        return document;
    }

    /**
     * Return set of unique URLs linked from this page. If there is
     *
     * @return set of unique URLs
     */
    @Override
    public Set<URI> getLinks() {
        Document doc = getDocument();
        Elements aElements = doc.select("a[href]");
        HashSet<URI> links = new HashSet<>();
        for (Element a: aElements) {
            try {
                links.add(new URI(a.attr("abs:href")));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return links;
    }

    /**
     * Saves parsed file content into file. Should be UTF8 encoded if possible.
     * Should be used only after saveContent.
     *
     * @param file Path to file
     * @return true if content was saved successfully
     * @throws ParserException On parsing error
     */
    @Override
    public boolean saveParsed(Path file) throws ParserException {
        try {
            Element body = getDocument().select("body").first();
            FileUtils.writeStringToFile(file.toFile(), body != null ? body.text() : "");
        } catch (IOException e) {
            logger.warning("IO exception for '" + url.toString() + "': " +e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Return file type extension
     *
     * @return extension as string without starting dot
     */
    @Override
    public String getExtension() {
        return "html";
    }

    /**
     * Check if URL can be parsed by this parser
     * @param url URL
     * @return true if URL can be parsed
     */
    static boolean canBeParsed(URI url) {
        return getMime(url).compareToIgnoreCase("text/html") == 0;
    }
}
