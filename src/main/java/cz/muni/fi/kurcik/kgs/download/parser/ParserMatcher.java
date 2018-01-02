package cz.muni.fi.kurcik.kgs.download.parser;

import java.net.URI;

/**
 * Class for finding matching parser for file
 * @author Lukáš Kurčík
 */
public interface ParserMatcher {
    /**
     * Finds matching parser for URL or return null
     * @param url URL
     * @return Parser object or null
     */
    Parser getParser(URI url);
}
