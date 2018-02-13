package cz.muni.fi.kurcik.kgs.util;

/**
 * Helper class for getting index of max value of array
 *
 * @author Lukáš Kurčík
 */
public class MaxIndex {
    /**
     * Return index of maximum
     *
     * @param array number array
     * @return index of first maximum occurrence
     */
    static public int max(double[] array) {
        int maxI = 0;
        for (int i = 0; i < array.length; ++i) {
            if (array[i] > array[maxI]) {
                maxI = i;
            }
        }
        return maxI;
    }
}
