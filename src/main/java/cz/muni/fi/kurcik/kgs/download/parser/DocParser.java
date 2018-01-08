package cz.muni.fi.kurcik.kgs.download.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Parser for Word .doc files
 * @author Lukáš Kurčík
 */
public class DocParser extends AbstractParser {
    private final static Logger logger = Logger.getLogger(DocParser.class.getName());

    protected HWPFDocument document;

    public DocParser(URI url) {
        super(url);
    }

    /**
     * Return HWPFDocument from doc file
     *
     * @return Document
     */
    protected HWPFDocument getDocument() {
        if (document != null)
            return document;

        try (InputStream is = new ByteArrayInputStream(getContent())) {
            document = new HWPFDocument(is);
        } catch (IOException e) {
            logger.warning("IO exception for '" + url.toString() + "': " + e.getMessage());
        }
        return document;
    }

    /**
     * Return set of unique URLs linked from this page. If there is
     *
     * @return set of unique URLs
     */
    @Override
    public Set<URI> getLinks() {
        HWPFDocument doc = getDocument();
        if (doc == null)
            return new HashSet<>();
        HashSet<URI> links = new HashSet<>();

        try {
            Document newDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            WordToHtmlConverter htmlConverter = new WordToHtmlConverter(newDocument);
            htmlConverter.processDocument(doc);

            StringWriter stringWriter = new StringWriter();

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.transform(new DOMSource(htmlConverter.getDocument()), new StreamResult(stringWriter));

            String html = stringWriter.toString();
            org.jsoup.nodes.Document htmlDocument = Jsoup.parse(html);
            Elements aElements = htmlDocument.select("a[href]");
            for (Element a : aElements) {
                try {
                    links.add(new URI(a.attr("abs:href")));
                } catch (URISyntaxException e) {
                    logger.warning(e.getMessage());
                }
            }
        } catch (ParserConfigurationException e) {
            logger.warning("DOC parser exception for '" + url.toString() + "': " + e.getMessage());
        } catch (TransformerException e) {
            logger.warning("Exception while parsing links'" + url.toString() + "': " + e.getMessage());
        }

        return links;
    }

    /**
     * Saves parsed file content into file. Should be UTF8 encoded if possible.
     * Should be used only after saveContent.
     *
     * @param file Path to file
     * @return true if content was saved successfully
     * @throws ParserException On parsing error
     */
    @Override
    public boolean saveParsed(Path file) throws ParserException {
        HWPFDocument doc = getDocument();
        if (doc == null)
            return false;

        try {
            FileUtils.writeStringToFile(file.toFile(), doc.getText().toString());
        } catch (IOException e) {
            logger.warning("IO exception for '" + url.toString() + "': " +e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Return file type extension
     *
     * @return extension as string without starting dot
     */
    @Override
    public String getExtension() {
        return "doc";
    }


    /**
     * Check if URL can be parsed by this parser
     *
     * @param url URL
     * @return true if URL can be parsed
     */
    static boolean canBeParsed(URI url) {
        return getMime(url).compareToIgnoreCase("application/msword") == 0;
    }
}
