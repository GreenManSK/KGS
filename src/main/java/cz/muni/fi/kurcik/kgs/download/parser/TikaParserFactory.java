package cz.muni.fi.kurcik.kgs.download.parser;

import java.net.URI;
import java.nio.file.Path;

/**
 * Factory for TikaParser
 * @author Lukáš Kurčík
 */
public class TikaParserFactory implements ParserFactory {

    public TikaParserFactory() {
    }

    /**
     * Create parser for specified URL, file should be local copy of its content
     *
     * @param url  URL for file
     * @param file File with saved content
     * @return Instance of Parser
     */
    @Override
    public Parser createParser(URI url, Path file) {
        return new TikaParser(url, file);
    }
}
