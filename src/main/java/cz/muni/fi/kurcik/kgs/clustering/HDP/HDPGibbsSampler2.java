package cz.muni.fi.kurcik.kgs.clustering.HDP;

import de.uni_leipzig.informatik.asv.hdp.HDPGibbsSampler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Extension to original HDPGibbsSampler from de.uni_leipzig.informatik.asv.hdp
 * @author Lukáš Kurčík
 */
public class HDPGibbsSampler2 extends HDPGibbsSampler {

    public HDPGibbsSampler2() {
        super();
    }


    /**
     * Saves model into file
     *
     * @param file
     * @throws IOException on problem with saving model
     */
    public void saveModel(Path file) throws IOException {
        try (PrintStream stream = new PrintStream(file.toString())) {
            for (int k = 0; k < numberOfTopics; k++) {
                for (int w = 0; w < sizeOfVocabulary; w++)
                    stream.format("%05d ", wordCountByTopicAndTerm[k][w]);
                stream.println();
            }
        } catch (FileNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * Return model build by sampler
     * @return model
     */
    public HDPModel getModel() {
        HDPModelBuilder builder = new HDPModelBuilder(numberOfTopics, totalNumberOfWords);
        for (int k = 0; k < numberOfTopics; k++) {
            for (int w = 0; w < sizeOfVocabulary; w++) {
                builder.addTopicWordCount(k, w, wordCountByTopicAndTerm[k][w]);
            }
        }
        return builder.build();
    }
}
