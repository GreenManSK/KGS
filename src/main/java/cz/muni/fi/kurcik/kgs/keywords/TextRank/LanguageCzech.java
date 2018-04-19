package cz.muni.fi.kurcik.kgs.keywords.TextRank;

import com.sharethis.textrank.LanguageModel;
import cz.muni.fi.kurcik.kgs.util.Majka;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of Czech-specific tools for NLP
 * Uses Majka
 *
 * @author Lukáš Kurčík
 */
public class LanguageCzech extends LanguageModel {

    private static final Logger logger = Logger.getLogger(LanguageCzech.class.getName());
    public static final String TAG_REGEX = "^.*:";

    protected final Majka majka;
    protected boolean allowUnknownWords;
    protected StanfordCoreNLP pipeline;

    protected final HashMap<String, List<String>> tokensCache = new HashMap<>();

    /**
     * Default constructor
     *
     * @param allowUnknownWords Specify if words that majka didn't tagged should be used
     */
    public LanguageCzech(boolean allowUnknownWords) {
        majka = new Majka();
        this.allowUnknownWords = allowUnknownWords;

        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit");
        pipeline = new StanfordCoreNLP(props);
    }

    /**
     * Load libraries for OpenNLP for this specific language.
     */
    @Override
    public void loadResources(final String path) throws Exception {
        // not needed
    }

    /**
     * Split sentences within the paragraph text.
     */
    @Override
    public String[] splitParagraph(final String text) {
        Annotation paragraph = new Annotation(text);
        pipeline.annotate(paragraph);


        // Lemmatize tokens
        Set<String> forMajka = new HashSet<>();

        List<String> sentenceList = new ArrayList<>();
        List<CoreMap> sentences = paragraph.get(CoreAnnotations.SentencesAnnotation.class);
        List<String> tokens;
        StringBuilder sentenceBuilder;
        for (CoreMap sentence : sentences) {
            sentenceBuilder = new StringBuilder();
            List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
            tokens = new ArrayList<>(labels.size());
            for (CoreLabel label : labels) {
                tokens.add(label.word());
                forMajka.add(label.word());
                sentenceBuilder.append(label.word().toLowerCase()).append(" ");
            }
            String strSentence = sentenceBuilder.toString().trim();
            tokensCache.put(strSentence, tokens);
            sentenceList.add(strSentence);
        }

        // This with majak cache will speed up tagging a lot, thanks to only one C++ call
        try {
            majka.findAll(new ArrayList<>(forMajka), Majka.IGNORE_CASE, true);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem while tagging tokens", e);
        }

        return sentenceList.toArray(new String[0]);
    }

    /**
     * Tokenize the sentence text into an array of tokens.
     */
    @Override
    public String[] tokenizeSentence(String text) {
        try {
            Map<String, String> lemmas = majka.findAll(tokensCache.get(text), Majka.IGNORE_CASE, false);
            String[] result = new String[tokensCache.get(text).size()];
            int i = 0;
            for (String s : tokensCache.get(text)) {
                result[i++] = lemmas.get(s);
            }
            return result;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem while tagging tokens", e);
        }
        return new String[0];
    }

    /**
     * Run a part-of-speech tagger on the sentence token list.
     */
    @Override
    public String[] tagTokens(final String[] token_list) {
        try {
            Map<String, String> tags = majka.findAll(Arrays.asList(token_list), Majka.IGNORE_CASE, true);
            String[] result = new String[token_list.length];
            for (int i = 0; i < token_list.length; i++) {
                result[i] = tags.get(token_list[i]).replaceAll(TAG_REGEX, "");
            }
            return result;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem while tagging tokens", e);
        }
        return new String[0];
    }

    /**
     * Prepare a stable key for a graph node (stemmed, lemmatized)
     * from a token.
     */
    @Override
    public String getNodeKey(final String text, final String pos) throws Exception {
        return (!pos.isEmpty() ? pos.substring(0, 2) : "") + stemToken(text).toLowerCase();
    }

    /**
     * Determine whether the given PoS tag is a noun.
     */
    @Override
    public boolean isNoun(String s) {
        return s.startsWith("k1");
    }

    /**
     * Determine whether the given PoS tag is an adjective.
     */
    @Override
    public boolean isAdjective(String s) {
        return s.startsWith("k2");
    }

    /**
     * Determine whether the given PoS tag is relevant to add to the
     * graph.
     */
    @Override
    public boolean isRelevant(String pos) {
        return pos.isEmpty() || isNoun(pos) || isAdjective(pos);
    }

    /**
     * Perform stemming on the given token.
     */
    @Override
    public String stemToken(String s) {
        return majka.getCache().get(s).replaceAll(TAG_REGEX, "");
    }
}
