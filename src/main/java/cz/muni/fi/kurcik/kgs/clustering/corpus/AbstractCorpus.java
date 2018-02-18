package cz.muni.fi.kurcik.kgs.clustering.corpus;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Abstract implementation of corpus
 *
 * @author Lukáš Kurčík
 */
abstract class AbstractCorpus implements Corpus {
    protected Vocabulary vocabulary;

    /**
     * Each document is represented as word ids and count for each word. nth number in array is word id, n+1th is ist count.
     */
    protected List<List<Integer>> documents;

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
                List<Integer> doc;
                String[] fields;
                fields = line.split("[ :]+");
                length = Integer.parseInt(fields[0]);
                doc = new ArrayList<>(length * 2);
                for (int n = 1; n < fields.length; n++) {
                    doc.add(Integer.parseInt(fields[n]));
                }
                documents.add(doc);
            });
        }
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
            for (List<Integer> doc : documents) {
                line = new StringBuilder();
                line.append(doc.size() / 2).append(" ");
                for (int i = 0; i < doc.size(); i += 2) {
                    if (i != 0)
                        line.append(" ");
                    line.append(doc.get(i)).append(":").append(doc.get(i + 1));
                }
                line.append('\n');
                fw.append(line.toString());
            }
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
            if (w.isEmpty())
                continue;
            Integer id = vocabulary.addAndGetId(w);
            counter.put(id, counter.get(id) != null ? counter.get(id) + 1 : 1);
        }

        List<Integer> doc = new ArrayList<>(counter.size() * 2);
        for (Map.Entry<Integer, Integer> entry : counter.entrySet()) {
            doc.add(entry.getKey());
            doc.add(entry.getValue());
        }
        documents.add(doc);
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
