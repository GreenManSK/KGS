package cz.muni.fi.kurcik.kgs.clustering.corpus;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Class used for working with document corpus.
 * File format:
 * [M] [term_1]:[count] [term_2]:[count] ...  [term_N]:[count]
 * where [M] is the number of unique terms in the document, and the
 * [count] associated with each term is how many times that term appeared
 * in the document.  Note that [term_1] is an integer which indexes the term; it is not a string.
 *
 * @author Lukáš Kurčík
 */
public class BasicCorpus extends AbstractCorpus {

    /**
     * Creates new empty corpus
     */
    public BasicCorpus() {
        vocabulary = new Vocabulary();
        documents = new LinkedList<>();
    }

    /**
     * Creates corpus from file and loads its vocabulary
     *
     * @param corpus     Corpus file
     * @param vocabulary Vocabulary file
     * @throws IOException On error while loading files
     */
    public BasicCorpus(Path corpus, Path vocabulary) throws IOException {
        documents = new LinkedList<>();
        loadCorpus(corpus);
        this.vocabulary = new Vocabulary(vocabulary);
    }

}
