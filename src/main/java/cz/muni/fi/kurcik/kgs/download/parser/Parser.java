package cz.muni.fi.kurcik.kgs.download.parser;

import java.io.File;
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
     * Get file to be parsed
     * @return File
     */
    Path getFile();

    /**
     * Return set of unique URLs linked from this page. If there is
     * @return set of unique URLs
     */
    Set<URI> getLinks();

    /**
     * Parse content from file into string
     *
     * @return String with file content
     * @throws ParserException On parsing error
     */
    String getContent() throws ParserException;

    /**
     * Check if File can be parsed by this parser
     * @return true if file can be parsed
     */
    boolean canBeParsed();


}
