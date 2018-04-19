package cz.muni.fi.kurcik.kgs.keywords.TextRank;

import com.sharethis.textrank.LanguageModel;

/**
 * Extension of TextRank by paco@sharethis.com so it could support custom LanguageModels
 * @author Lukáš Kurčík
 */
public class TextRank extends com.sharethis.textrank.TextRank {

    /**
     * Create new TextRank
     * @param languageModel languageModel
     * @throws Exception if something goes wrong with odl TextRank constructor, probably should be cached and ignored
     */
    public TextRank(LanguageModel languageModel) throws Exception {
        lang = languageModel;
    }
}
