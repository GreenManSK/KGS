package cz.muni.fi.kurcik.kgs;

import cz.muni.fi.kurcik.kgs.download.BasicDownloader;
import cz.muni.fi.kurcik.kgs.download.Downloader;
import cz.muni.fi.kurcik.kgs.download.parser.TikaParserFactory;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.langdetect.OptimaizeLangDetector;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author Lukáš Kurčík
 */
public class Main {

    public static void main(String[] args) throws IOException, URISyntaxException {
        Logger logger = Logger.getLogger("downloadLogger");

        Path path = Paths.get("W:/Baka/mine");
        FileHandler fh = new FileHandler(path.resolve("download.log").toString());
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);

        Downloader downloader = new BasicDownloader(
                "cs",
                new TikaParserFactory(),
                new OptimaizeLangDetector(),
                logger);


        downloader.setDownloadDirectory(path);
        downloader.downloadPage(new URI("http://localhost/baka/"), 0, 1);
    }
}
