package cz.muni.fi.kurcik.kgs.clustering.HDP;

import com.drew.lang.Charsets;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for HDPModel
 */
public class HDPModelBuilder {
    protected int topics = 0;
    protected int words = 0;
    protected List<int[]> topicToWordCounts;
    protected int[] wordCount;

    /**
     * Creates new builder for model
     *
     * @param topics Number of topics in model
     * @param words  Number of words in model
     */
    public HDPModelBuilder(int topics, int words) {
        this.topics = topics;
        this.words = words;
        topicToWordCounts = new ArrayList<>();
        for (int i = 0; i < topics; ++i) {
            topicToWordCounts.add(new int[words]);
        }
        wordCount = new int[words];
    }

    /**
     * Adds word count for specified word into topic
     *
     * @param topicId Topic id
     * @param wordId  Word id
     * @param count   word count
     * @return builder
     */
    public HDPModelBuilder addTopicWordCount(int topicId, int wordId, int count) {
        topicToWordCounts.get(topicId)[wordId] = count;
        wordCount[wordId] += count;
        return this;
    }

    /**
     * Add new topic
     *
     * @param topicId    topic id
     * @param wordCounts array with counts for all words
     * @return builder
     */
    public HDPModelBuilder addTopic(int topicId, int[] wordCounts) {
        for (int i = 0; i < words; i++) {
            addTopicWordCount(topicId, i, wordCount[i]);
        }
        return this;
    }

    /**
     * Builds model
     *
     * @return HDP model
     */
    public HDPModel build() {
        return new HDPModel(topics, words, topicToWordCounts, wordCount);
    }

    /**
     * Builds model from file
     *
     * @param model path to model file
     * @return HDP model
     * @throws IOException on problem with file IO
     */
    static public HDPModel buildFromFile(Path model) throws IOException {
        HDPModelBuilder builder = null;
        List<String> lines = FileUtils.readLines(model.toFile(), Charsets.UTF_8);
        int topicId = 0;
        for (String line : lines) {
            List<Integer> wordCounts = Arrays.asList(line.split("\\s")).stream().map(Integer::parseInt).collect(Collectors.toList());
            if (builder == null) {
                builder = new HDPModelBuilder(lines.size(), wordCounts.size());
            }
            for (int i = 0; i < wordCounts.size(); i++) {
                builder.addTopicWordCount(topicId, i, wordCounts.get(i));
            }
            topicId++;
        }
        return builder.build();
    }
}