package cz.muni.fi.kurcik.kgs.download.parser;


import org.apache.commons.io.FileUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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

/**
 * Parser for Word .doc files
 *
 * @author Lukáš Kurčík
 */
public class DocxParser extends AbstractParser {
    private final static Logger logger = Logger.getLogger(DocxParser.class.getName());

    private XWPFDocument document;

    public DocxParser(URI url) {
        super(url);
    }

    /**
     * Return XWPFDocument from docx file
     *
     * @return Document
     */
    protected XWPFDocument getDocument() {
        if (document != null)
            return document;

        try (InputStream is = new ByteArrayInputStream(getContent())) {
            document = new XWPFDocument(is);
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
        XWPFDocument doc = getDocument();
        if (doc == null)
            return new HashSet<>();
        HashSet<URI> links = new HashSet<>();

        for (XWPFParagraph p : doc.getParagraphs()) {
            for (XWPFRun run : p.getRuns()) {
                if (run instanceof XWPFHyperlinkRun) {
                    XWPFHyperlink link = ((XWPFHyperlinkRun) run).getHyperlink(doc);
                    if (link != null) {
                        try {
                            links.add(new URI(link.getURL()));
                        } catch (URISyntaxException e) {
                            logger.warning(e.getMessage());
                        }
                    }
                }
            }
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
        XWPFDocument doc = getDocument();
        if (doc == null)
            return false;

        try {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            FileUtils.writeStringToFile(file.toFile(), extractor.getText());
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
        return "docx";
    }


    /**
     * Check if URL can be parsed by this parser
     *
     * @param url URL
     * @return true if URL can be parsed
     */
    static boolean canBeParsed(URI url) {
        return getMime(url).matches(".*wordprocessingml.document.*");
    }

}
