package cz.muni.fi.kurcik.kgs.clustering.corpus;

import com.drew.lang.Charsets;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private int idCounter = 0;
    protected final HashMap<String, Integer> word2id;
    protected final HashMap<Integer, String> id2word;

    /**
     * Creates empty vocabulary
     */
    public Vocabulary() {
        word2id = new HashMap<>();
        id2word = new HashMap<>();
    }

    /**
     * Construct new vocabulary based on old one but generates new word ids
     * @param old Old vocabulary
     */
    public Vocabulary(Vocabulary old) {
        this();
        for (String word : old.word2id.keySet()) {
            addWord(word);
        }
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
        word = word.toLowerCase();
        if (!word2id.containsKey(word)) {
            word2id.put(word, idCounter);
            id2word.put(idCounter++, word);
        }
    }

    /**
     * Gets id for word. Return null if word is not in vocabulary
     *
     * @param word
     * @return Id for word or null
     */
    public Integer getId(String word) {
        word = word.toLowerCase();
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
        return getId(word);
    }

    /**
     * Removes word from vocabulary.
     *
     * @param word word
     */
    public void remove(String word) {
        id2word.remove(getId(word));
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
        return word2id.size();
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

    /**
     * Return pairs of id and words
     * @return word-id pairing map
     */
    public Map<Integer, String> getPairs() {
        return Collections.unmodifiableMap(id2word);
    }
}
