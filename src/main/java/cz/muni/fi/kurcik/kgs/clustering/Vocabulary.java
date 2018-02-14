package cz.muni.fi.kurcik.kgs.clustering;

import com.drew.lang.Charsets;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for working with documents vocabulary used in clustering
 * <p>
 * File format:
 * Each line should contain one word. Lines are used as word id and are counted from 0
 *
 * @author Lukáš Kurčík
 */
public class Vocabulary {

    protected final BidiMap<String, Integer> word2id;
    protected final BidiMap<Integer, String> id2word;

    /**
     * Creates empty vocabulary
     */
    public Vocabulary() {
        word2id = new DualHashBidiMap<>();
        id2word = word2id.inverseBidiMap();
    }

    /**
     * Creates vocabulary from file
     *
     * @param vocabulary Path to file with vocabulary
     * @throws IOException on error while loading words from file
     */
    public Vocabulary(Path vocabulary) throws IOException {
        this();
        loadVocabulary(vocabulary);
    }

    /**
     * Loads vocabulary from file
     *
     * @param vocabulary Path to file with vocabulary
     * @throws IOException on error while loading words from file
     */
    protected void loadVocabulary(Path vocabulary) throws IOException {
        List<String> lines = FileUtils.readLines(vocabulary.toFile(), Charsets.UTF_8);
        lines.forEach(line -> {
            if (line.isEmpty())
                return;
            String[] parts = line.split(" ");
            word2id.put(parts[1], Integer.parseInt(parts[0]));
        });
    }

    /**
     * Adds new word into vocabulary
     *
     * @param word
     */
    public void addWord(String word) {
        if (!word2id.containsKey(word)) {
            word2id.put(word, id2word.size() - 1);
        }
    }

    /**
     * Gets id for word. Return null if word is not in vocabulary
     *
     * @param word
     * @return Id for word or null
     */
    public Integer getId(String word) {
        return word2id.get(word);
    }

    /**
     * Gets id for word. If word is not in vocabulary, it will be added
     *
     * @param word word
     * @return Id
     */
    public Integer addAndGetId(String word) {
        Integer id = getId(word);
        if (id != null)
            return id;
        addWord(word);
        return word2id.size() - 1;
    }

    /**
     * Removes word from vocabulary.
     *
     * @param word word
     */
    public void remove(String word) {
        word2id.remove(word);
    }

    /**
     * Return word for id or null.
     *
     * @param id
     * @return word or null
     */
    public String getWord(Integer id) {
        return id < id2word.size() ? id2word.get(id) : null;
    }

    /**
     * Return size of vocabulary
     *
     * @return number of words in vocabulary
     */
    public int size() {
        return id2word.size();
    }

    /**
     * Saves vocabulary into file
     *
     * @param file Path to file
     * @throws IOException When there is problem with saving
     */
    public void save(Path file) throws IOException {
        FileUtils.writeLines(file.toFile(), word2id.entrySet().stream().map(it -> it.getValue() + " " + it.getKey()).collect(Collectors.toList()));
    }
}
