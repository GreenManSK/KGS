package cz.muni.fi.kurcik.kgs.clustering;

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
 */
public class Corpus {
    final protected Vocabulary vocabulary;

    /**
     * Each document is represented as word ids and count for each word. nth number in array is word id, n+1th is ist count.
     */
    final protected List<int[]> documents = new LinkedList<>();

    /**
     * Creates new empty corpus
     */
    public Corpus() {
        vocabulary = new Vocabulary();
    }

    /**
     * Creates corpus from file and loads its vocabulary
     *
     * @param corpus     Corpus file
     * @param vocabulary Vocabulary file
     * @throws IOException On error while loading files
     */
    public Corpus(Path corpus, Path vocabulary) throws IOException {
        loadCorpus(corpus);
        this.vocabulary = new Vocabulary(vocabulary);
    }

    /**
     * Loads corpus from file
     *
     * @param corpus Path to file
     * @throws IOException On file io error
     */
    protected void loadCorpus(Path corpus) throws IOException {
        try (Stream<String> stream = Files.lines(corpus)) {
            stream.forEach(line -> {
                int length, word, counts;
                int[] doc;
                String[] fields;
                fields = line.split("[ :]+");
                length = Integer.parseInt(fields[0]);
                doc = new int[length * 2];
                for (int n = 1; n < fields.length; n++) {
                    doc[n - 1] = Integer.parseInt(fields[n]);
                }
                documents.add(doc);
            });
        }
    }

    /**
     * Adds array of words as new document
     *
     * @param words All words in document
     */
    public void addDocument(String[] words) {
        Map<Integer, Integer> counter = new HashMap<>();
        for (String w : words) {
            if (w.length() <= 0)
                continue;
            Integer id = vocabulary.addAndGetId(w);
            counter.put(id, counter.get(id) != null ? counter.get(id) + 1 : 1);
        }

        int[] doc = new int[counter.size() * 2];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : counter.entrySet()) {
            doc[i++] = entry.getKey();
            doc[i++] = entry.getValue();
        }
        documents.add(doc);
    }

    /**
     * Saves corpus into file
     *
     * @param file Path to file
     * @throws IOException When there is problem with saving
     */
    public void save(Path file) throws IOException {
        StringBuilder line;
        try (FileWriter fw = new FileWriter(file.toFile())) {
            for (int[] doc : documents) {
                line = new StringBuilder();
                line.append(doc.length / 2).append(" ");
                for (int i = 0; i < doc.length; i += 2) {
                    if (i != 0)
                        line.append(" ");
                    line.append(doc[i]).append(":").append(doc[i + 1]);
                }
                line.append('\n');
                fw.append(line.toString());
            }
        }
    }

    /**
     * Get vocabulary
     *
     * @return vocabulary
     */
    public Vocabulary getVocabulary() {
        return vocabulary;
    }
}
