package cz.muni.fi.kurcik.kgs.download.parser;

import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

/**
 * Interface for parsing url and their content
 * @author Lukáš Kurčík
 */
public interface Parser {
    /**
     * Get url parsed by this object
     * @return URL
     */
    URI getUrl();

    /**
     * Return set of unique URLs linked from this page. If there is
     * @return set of unique URLs
     */
    Set<URI> getLinks();

    /**
     * Saves file content into file.
     * @param file Path to file
     * @return true if content was saved successfully
     * @throws ParserException On parsing error
     */
    boolean saveContent(Path file) throws ParserException;

    /**
     * Saves parsed file content into file. Should be UTF8 encoded if possible.
     * @param file Path to file
     * @return true if content was saved successfully
     * @throws ParserException On parsing error
     */
    boolean saveParsed(Path file) throws ParserException;

    /**
     * Check if URL can be parsed by this parser
     * @return true if URL can be parsed
     */
    static boolean canBeParsed(URI url) {
        return true;
    }
}
