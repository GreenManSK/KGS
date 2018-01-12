package cz.muni.fi.kurcik.kgs.download;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Interface for page downloader
 *
 * @author Lukáš Kurčík
 */
public interface Downloader {
    String ORIGINAL_FILES_DIR = "original";
    String PARSED_FILES_DIR = "parsed";
    String PARSED_EXTENSION = ".txt";
    String LINKS_EXTENSION = ".links";

    /**
     * Downloads all files with supported formats from this domain. Data will be put into folder named after domain.
     * Files are downloaded only form specified domain and all domains that are number of specified hops away from this domain.
     * Downloads only files till specified depth, url is depth 0.
     *
     * Each file have its original content saved into original/ID.extension and parsed content as parsed/ID.txt, where ID is assigned by Downloader.
     * URLs linked from site are put into ID.links file, each URL on separate line.
     * ID and URL pairs are saved into ids.txt in format [ID] [URL], each on separate line.
     *
     * @param url   Web page url
     * @param hops  Number of hops to other domains
     * @param depth Depth of file downloading
     * @throws IOException when there is problem with downloading
     */
    void downloadPage(URI url, int hops, int depth) throws IOException;

    /**
     * Sets download directory for downloader. All data will be put into dirName/url
     *
     * @param dir Directory to download folder
     */
    void setDownloadDirectory(Path dir);

    /**
     * Returns path to folder with downloaded data
     *
     * @return directory to download folder
     */
    Path getDownloadDirectory();
}
