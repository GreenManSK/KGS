package cz.muni.fi.kurcik.kgs.download.parser;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.BoilerpipeContentHandler;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.LinkContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Parser interface implementation using org.apache.tika library
 *
 * @author Lukáš Kurčík
 */
public class TikaParser implements Parser {

    private static final Logger logger = Logger.getLogger(TikaParser.class.getName());
    protected URI url;
    protected Path file;
    protected Metadata metadata;

    protected boolean useContentDetection = true;

    private org.apache.tika.parser.Parser parser;
    private ContentHandler bodyHandler = new BodyContentHandler();
    private boolean bodyParsed = false;

    /**
     * Constructor for new TikaParser
     *
     * @param url
     * @param file
     */
    public TikaParser(URI url, Path file) {
        this(url, file, false);
    }

    /**
     * Constructor for new TikaParser
     *
     * @param url
     * @param file
     * @param useContentDetection Specify if content detection for HTML should be used
     */
    public TikaParser(URI url, Path file, boolean useContentDetection) {
        this.useContentDetection = useContentDetection;
        this.file = file;
        this.url = url;

        this.metadata = new Metadata();
        this.metadata.set(Metadata.RESOURCE_NAME_KEY, url.getPath());
    }

    /**
     * Get url parsed by this object
     *
     * @return URL
     */
    @Override
    public URI getUrl() {
        return url;
    }

    /**
     * Get file to be parsed
     *
     * @return File
     */
    @Override
    public Path getFile() {
        return file;
    }

    /**
     * Return set of unique URLs linked from this page. If there is
     *
     * @return set of unique URLs
     */
    @Override
    public Set<URI> getLinks() {
        LinkContentHandler linkHandler = new LinkContentHandler();
        try {
            parse(linkHandler);
            return linkHandler.getLinks().stream().map(link -> {
                try {
                    URI linkUri = URI.create(link.getUri());
                    if (linkUri.isAbsolute())
                        return url.resolve(linkUri);
                    else {
                        return url.resolve((!linkUri.toString().startsWith("/") ? "/" : "") + linkUri);
                    }
                } catch (IllegalArgumentException e) {
                    return url;
                }
            }).map(uri -> {
                if (uri.getPath() == null || uri.getPath().isEmpty())
                    return uri.resolve("/");
                else
                    return uri;
            }).filter(URI::isAbsolute).collect(Collectors.toSet());
        } catch (ParserException e) {
        }
        return Collections.emptySet();
    }

    /**
     * Parse content from file into string
     *
     * @return String with file content
     * @throws ParserException On parsing error
     */
    @Override
    public String getContent() throws ParserException {
        if (!bodyParsed) {
            parse(getBodyHandler());
        }

        String title = "";
        if (metadata.get(Metadata.TITLE) != null) {
            title = metadata.get(Metadata.TITLE) + "\n\n";
        }
        return title + bodyHandler.toString();
    }

    /**
     * Check if File can be parsed by this parser
     *
     * @return true if file can be parsed
     */
    @Override
    public boolean canBeParsed() {
        try {
            if (bodyParsed)
                return true;
            parse(getBodyHandler());
            bodyParsed = true;
        } catch (ParserException e) {
            return false;
        }
        return true;
    }

    /**
     * Parse body of file
     *
     * @param handler Handler for context
     * @throws ParserException If there is problem while parsing
     */
    protected void parse(ContentHandler handler) throws ParserException {
        if (parser == null)
            parser = new AutoDetectParser();

        try (TikaInputStream input = TikaInputStream.get(file)) {
            parser.parse(input, handler, metadata, new ParseContext());
        } catch (IOException e) {
            logger.log(Level.WARNING, "IO exception while parsing " + url, e);
            throw new ParserException(e);
        } catch (TikaException | SAXException e) {
            throw new ParserException(e);
        }
    }

    /**
     * Finds extension for provided mime type or returns .ukw
     *
     * @param mime
     * @return file extension with .
     */
    public static String extensionFromMime(String mime) {
        try {
            return MimeTypes.getDefaultMimeTypes().forName(mime).getExtension();
        } catch (MimeTypeException e) {
            return ".ukw";
        }
    }

    /**
     * Return right BodyHandler based on type of file. Use content detector for HTML
     *
     * @return body handler
     */
    protected ContentHandler getBodyHandler() {
        if (!useContentDetection)
            return bodyHandler;
        return FilenameUtils.getExtension(file.toString()).compareToIgnoreCase("html") != 0 ? bodyHandler : new BoilerpipeContentHandler(bodyHandler);
    }

    /**
     * Check if content detection is enabled
     *
     * @return true if yes
     */
    public boolean isUseContentDetection() {
        return useContentDetection;
    }

    /**
     * Set content detection for html pages
     *
     * @param useContentDetection true is should be used
     */
    public void setUseContentDetection(boolean useContentDetection) {
        this.useContentDetection = useContentDetection;
    }
}
