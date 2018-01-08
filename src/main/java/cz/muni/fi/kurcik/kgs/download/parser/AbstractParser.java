package cz.muni.fi.kurcik.kgs.download.parser;

import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Abstract parser with some useful functions
 *
 * @author Lukáš Kurčík
 */
abstract public class AbstractParser implements Parser {

    private final static Logger logger = Logger.getLogger(AbstractParser.class.getName());

    final protected URI url;
    private byte[] content;
    private File contentFile;

    protected AbstractParser() {
        url = null;
    }

    public AbstractParser(URI url) {
        this.url = url;
    }

    /**
     * Saves file content into file.
     *
     * @param file Path to file
     * @return true if content was saved successfully
     */
    @Override
    public boolean saveContent(Path file) {
        contentFile = file.toFile();
        try (
                InputStream in = url.toURL().openStream();
                ReadableByteChannel rbc = Channels.newChannel(in);
                FileOutputStream fos = new FileOutputStream(contentFile)
        ) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (MalformedURLException e) {
            logger.warning("Malformed URL exception for '" + url.toString() + "'");
            return false;
        } catch (IOException e) {
            logger.warning("IO exception for '" + url.toString() + "': " +e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Should be used for getting content of file for parsing. Use only after saveContent function.
     * @return byte array of file content
     */
    protected byte[] getContent() {
        if (contentFile == null)
            return null;
        if (content != null)
            return content;
        try (FileInputStream fis = new FileInputStream(contentFile)) {
            content = IOUtils.toByteArray(fis);
        } catch (FileNotFoundException e) {
            logger.warning("File not found for '" + contentFile.toString() + "': " +e.getMessage());
        } catch (IOException e) {
            logger.warning("IO exception for '" + contentFile.toString() + "': " +e.getMessage());
        }
        return content;
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
     * Get mime type of file located at url
     * @param url URL
     * @return mime of empty string if mime can't be obtained
     */
    static protected String getMime(URI url) {
        try  {
            URLConnection connection = url.toURL().openConnection();
            return connection.getContentType();
        } catch (MalformedURLException e) {
            logger.warning("Malformed URL exception for '" + url.toString() + "'");
        } catch (IOException e) {
            logger.warning("IO exception for '" + url.toString() + "': " +e.getMessage());
        }
        return "";
    }
}
