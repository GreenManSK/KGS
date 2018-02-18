package cz.muni.fi.kurcik.kgs.clustering.corpus;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for corpus builders
 * File format:
 * [M] [term_1]:[count] [term_2]:[count] ...  [term_N]:[count]
 * where [M] is the number of unique terms in the document, and the
 * [count] associated with each term is how many times that term appeared
 * in the document.  Note that [term_1] is an integer which indexes the term; it is not a string.
 *
 * @author Lukáš Kurčík
 */
public interface Corpus {
    /**
     * Adds array of words as new document
     *
     * @param words All words in document
     */
    void addDocument(String[] words);

    /**
     * Saves corpus into file
     *
     * @param file Path to file
     * @throws IOException When there is problem with saving
     */
    public void save(Path file) throws IOException;

    /**
     * Get vocabulary
     *
     * @return vocabulary
     */
    public Vocabulary getVocabulary();
}
