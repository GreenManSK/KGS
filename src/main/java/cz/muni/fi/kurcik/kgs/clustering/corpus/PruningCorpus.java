package cz.muni.fi.kurcik.kgs.clustering.corpus;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Corpus builder that uses pruning of vocabulary for memory and clustering efficiency
 * Pruning rate specify at which percentage of processed documents will be words with occurrence count equals to 1 removed.
 * Vocabulary size could be larger that wanted size. Words with same occurrence count as last word will be left untouched.
 *
 * @author Lukáš Kurčík
 */
public class PruningCorpus extends AbstractCorpus {

    final protected Logger logger;
    final protected double pruningRate;
    protected long docCount = 0;
    final protected long wantedSize;
    final protected double redundantPercentage;

    protected double lastPrune = 0;
    protected long documentsParsed = 0;

    final HashMap<String, Long> wordCounter = new HashMap<>();
    final HashMap<String, Long> wordInDocCounter = new HashMap<>();

    /**
     * Constructs new corpus builder. There will be only one pruning at the end of building.
     *
     * @param wantedSize Wanted size of vocabulary
     */
    public PruningCorpus(double redundantPercentage, long wantedSize) {
        this(0, redundantPercentage, wantedSize);
    }

    /**
     * Constructs new corpus builder. There will be only one pruning at the end of building.
     *
     * @param redundantPercentage Specify percentage of documents, that contains words for word to be dropped
     * @param wantedSize          Wanted size of vocabulary
     * @param logger              Logger
     */
    public PruningCorpus(double redundantPercentage, long wantedSize, Logger logger) {
        this(0, redundantPercentage, wantedSize, logger);
    }

    /**
     * Constructs new corpus builder
     *
     * @param pruningRate         Pruning rate in percents
     * @param redundantPercentage Specify percentage of documents, that contains words for word to be dropped
     * @param wantedSize          Wanted size of vocabulary
     */
    public PruningCorpus(double pruningRate, double redundantPercentage, long wantedSize) {
        this(pruningRate, redundantPercentage, wantedSize, Logger.getLogger(PruningCorpus.class.getName()));
    }

    /**
     * Constructs new corpus builder
     *
     * @param pruningRate         Pruning rate in percents
     * @param redundantPercentage Specify percentage of documents, that contains words for word to be dropped
     * @param wantedSize          Wanted size of vocabulary
     * @param logger              Logger
     */
    public PruningCorpus(double pruningRate, double redundantPercentage, long wantedSize, Logger logger) {
        this.logger = logger;
        this.pruningRate = pruningRate;
        this.wantedSize = wantedSize;
        this.redundantPercentage = redundantPercentage;
        vocabulary = new Vocabulary();
        documents = new ArrayList<>();
    }

    /**
     * Adds array of words as new document
     *
     * @param words All words in document
     */
    @Override
    public void addDocument(String[] words) {
        super.addDocument(words);
        HashSet<String> countedWords = new HashSet<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                wordCounter.compute(word, (k, v) -> v == null ? 1 : v + 1);
                if (!countedWords.contains(word)) {
                    wordInDocCounter.compute(word, (k, v) -> v == null ? 1 : v + 1);
                }
            }
        }
        documentsParsed++;
        prune();
    }

    /**
     * Saves corpus into file
     *
     * @param file Path to file
     * @throws IOException When there is problem with saving
     */
    @Override
    public void save(Path file) throws IOException {
        removeRedundant();
        resizeVocabulary();
        normalize();
        super.save(file);
    }

    /**
     * Normalize vocabulary so that no word id is larger that vocabulary size
     */
    protected void normalize() {
        Vocabulary newVocabulary = new Vocabulary(vocabulary);
        HashMap<Integer, Integer> translator = new HashMap<>();
        for (Map.Entry<Integer, String> entry : vocabulary.getPairs().entrySet()) {
            translator.put(entry.getKey(), newVocabulary.getId(entry.getValue()));

        }

        this.vocabulary = newVocabulary;

        List<List<Integer>> newDocuments = new ArrayList<>();

        for (List<Integer> document : documents) {
            newDocuments.add(
                    IntStream.range(0, document.size()).mapToObj(i -> {
                        Integer word = document.get(i);
                        return i % 2 == 0 ? translator.get(word) : word;
                    }).collect(Collectors.toList())
            );
        }

        this.documents = newDocuments;
    }

    /**
     * Remove redundant words
     */
    protected void removeRedundant() {
        logger.info("Removing redundant words with percentage " + (redundantPercentage * 100) + "% and higher, actual size = " + vocabulary.size());
        LinkedHashMap<String, Long> sortedCounter = wordInDocCounter.entrySet().stream()
                .sorted((Comparator<Map.Entry<String, Long>> & Serializable) (c1, c2) -> c2.getValue().compareTo(c1.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        HashSet<Integer> wordToRemove = new HashSet<>();
        for (Map.Entry<String, Long> e : sortedCounter.entrySet()) {
            if (e.getValue() / documents.size() >= redundantPercentage) {
                wordToRemove.add(vocabulary.getId(e.getKey()));
                vocabulary.remove(e.getKey());
                wordCounter.remove(e.getKey());
            }
        }

        logger.info("Wanting to remove " + wordToRemove.size() + " words");

        removeWords(wordToRemove);
        logger.info("Vocabulary resized, actual size = " + vocabulary.size());
    }

    /**
     * Resize vocabulary
     */
    protected void resizeVocabulary() {
        logger.info("Resizing vocabulary, actual size = " + vocabulary.size());

        LinkedHashMap<String, Long> sortedCounter = wordCounter.entrySet().stream()
                .sorted((Comparator<Map.Entry<String, Long>> & Serializable) (c1, c2) -> c2.getValue().compareTo(c1.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        long checked = 0;
        long minCount = 0;
        HashSet<Integer> wordToRemove = new HashSet<>();

        for (Map.Entry<String, Long> e : sortedCounter.entrySet()) {
            checked++;
            if (checked > wantedSize && minCount != e.getValue()) {
                wordToRemove.add(vocabulary.getId(e.getKey()));
                vocabulary.remove(e.getKey());
                wordCounter.remove(e.getKey());
            } else if (checked == wantedSize) {
                minCount = e.getValue();
            }
        }
        logger.info("Wanting to remove " + wordToRemove.size() + " words");

        removeWords(wordToRemove);
        logger.info("Vocabulary resized, actual size = " + vocabulary.size());
    }

    /**
     * Check if pruning should be done and prune
     */
    protected void prune() {
        double parsed = (100 * documentsParsed / docCount);
        if (pruningRate > 0 && parsed > lastPrune + pruningRate) {
            lastPrune += pruningRate;
            logger.info("Pruning at " + documentsParsed + " documents parsed from " + docCount);
            HashSet<Integer> wordToRemove = new HashSet<>();

            for (Map.Entry<String, Long> e : wordCounter.entrySet()) {
                if (e.getValue() == 1) {
                    wordToRemove.add(vocabulary.getId(e.getKey()));
                    vocabulary.remove(e.getKey());
                }
            }
            wordCounter.entrySet().removeIf(e -> e.getValue() == 1);

            if (wordToRemove.size() == 0) {
                logger.info("No words removed");
                return;
            }
            removeWords(wordToRemove);
            logger.info("Removed " + wordToRemove.size() + " words");
        }
    }

    /**
     * Removes words from documents
     *
     * @param wordToRemove
     */
    protected void removeWords(HashSet<Integer> wordToRemove) {
        List<List<Integer>> newDocuments = new ArrayList<>();
        documents.forEach(doc -> {
            ArrayList<Integer> newDoc = new ArrayList<>();
            for (int i = 0; i < doc.size(); i += 2) {
                if (!wordToRemove.contains(doc.get(i))) {
                    newDoc.add(doc.get(i));
                    newDoc.add(doc.get(i + 1));
                }
            }
            newDocuments.add(newDoc);
        });

        documents = newDocuments;
    }

    public void setDocCount(long docCount) {
        this.docCount = docCount;
    }
}
