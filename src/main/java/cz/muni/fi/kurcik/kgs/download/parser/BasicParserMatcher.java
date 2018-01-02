package cz.muni.fi.kurcik.kgs.download.parser;

import java.net.URI;

/**
 * Basic implementation of ParserMatcher
 * @author Lukáš Kurčík
 */
public class BasicParserMatcher implements ParserMatcher {
    /**
     * Finds matching parser for URL or return null
     *
     * @param url URL
     * @return Parser object or null
     */
    @Override
    public Parser getParser(URI url) {
        return null;
    }
}
