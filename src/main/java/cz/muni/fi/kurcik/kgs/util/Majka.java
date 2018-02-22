package cz.muni.fi.kurcik.kgs.util;

import cz.muni.fi.kurcik.kgs.preprocessing.MajkaPreprocessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java wrapper for C++ majka library
 * See https://nlp.fi.muni.cz/czech-morphology-analyser/ for more information
 *
 * @author Lukáš Kurčík
 */
public class Majka {
    public static final int ADD_DIACRITICS = 1;
    public static final int IGNORE_CASE = 2;
    public static final int DISALLOW_LOWERCASE = 4;

    private final Logger logger = Logger.getLogger(Majka.class.getName());
    protected static final String TAGS_REGEX = ":.*$";
    protected static final String DICTIONARY_FILE = "majka/majka.w-lt";

    /**
     * Return all lemmas with tags for word.
     *
     * @param dict  path to dictionary
     * @param word  word
     * @param flags Flags for majka as integer
     * @return array with lemma:tag results
     */
    protected native String[] find(String dict, String word, int flags);

    /**
     * Return first lemma:tag pair for each word
     *
     * @param dict      path to dictionary
     * @param words     array with words
     * @param wordCount Length of words array
     * @param flags     Flags for majka as integer
     * @return array with lemma:tag results
     */
    protected native String[] findAll(String dict, String[] words, int wordCount, int flags);

    static {
        System.loadLibrary("majkaj"); /* Loads libmajkaj.so for unix or libmajkaj.dll for windows */
    }

    public Majka() {
    }

    /**
     * Return all lemmas with tags for word.
     *
     * @param word  word
     * @param flags Flags for majka as integer
     * @param tags  Specify if tags should be part of lemma string
     * @return List with lemma:tag results
     * @throws IOException on problem with dictionary
     */
    public List<String> find(String word, int flags, boolean tags) throws IOException {
        Path dic = unpackDictionary();
        Stream<String> output = Arrays.stream(find(dic.toString(), word, flags));

        if (!tags) {
            output = output.map(it -> it.replaceFirst(TAGS_REGEX, ""));
        }

        List<String> result = output.collect(Collectors.toList());
        Files.delete(dic);

        return result;
    }

    /**
     * Return first lemma:tag pair for each word
     *
     * @param words List with words
     * @param flags Flags for majka as integer
     * @param tags  Specify if tags should be part of lemma string
     * @return Map with word -> lemma mapping
     * @throws IOException on problem with dictionary
     */
    public Map<String, String> findAll(List<String> words, int flags, boolean tags) throws IOException {
        Path dic = unpackDictionary();

        String[] wordsArray;
        wordsArray = (new HashSet<>(words)).toArray(new String[0]); // For better performance use only unique words

        String[] output = findAll(dic.toString(), wordsArray, wordsArray.length, flags);
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < wordsArray.length; i++) {
            result.put(wordsArray[i], tags ? output[i] : output[i].replaceFirst(TAGS_REGEX, ""));
        }
        Files.delete(dic);

        return result;
    }

    /**
     * Unpack dictionary from resources
     *
     * @return path to dictionary
     * @throws IOException If there is problem with unpacking
     */
    protected Path unpackDictionary() throws IOException {
        try {
            Path file = Files.createTempFile("kgs-", "-dic");
            try (InputStream input = MajkaPreprocessor.class.getClassLoader().getResourceAsStream(DICTIONARY_FILE);
                 OutputStream output = new FileOutputStream(file.toFile())) {
                IOUtils.copy(input, output);
            }
            return file;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while creating dictionary for Majka", e);
            throw e;
        }
    }
}
