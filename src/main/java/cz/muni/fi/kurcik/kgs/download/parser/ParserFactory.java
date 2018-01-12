package cz.muni.fi.kurcik.kgs.download.parser;

import java.net.URI;
import java.nio.file.Path;

/**
 * Interface for parser factory
 * @author Lukáš Kurčík
 */
public interface ParserFactory {

    /**
     * Create parser for specified URL, file should be local copy of its content
     * @param url URL for file
     * @param file File with saved content
     * @return Instance of Parser
     */
    Parser createParser(URI url, Path file);
}
