package cz.muni.fi.kurcik.kgs.download.parser;

import java.net.URI;
import java.util.HashSet;

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
        if (HtmlParser.canBeParsed(url))
            return new HtmlParser(url);
        if (DocParser.canBeParsed(url))
            return new DocParser(url);
        return null;
    }
}
