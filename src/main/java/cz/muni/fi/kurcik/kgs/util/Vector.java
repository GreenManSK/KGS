package cz.muni.fi.kurcik.kgs.util;

import java.util.stream.DoubleStream;

/**
 * Util class for math vectors
 *
 * @author Lukáš Kurčík
 */
final public class Vector {
    private double[] vec;

    /**
     * Creates new vector
     *
     * @param vec vector array
     */
    public Vector(double[] vec) {
        this.vec = vec;
    }

    /**
     * Use function on each vector element
     *
     * @param function mapping function
     * @return new vector
     */
    public Vector use(UseFunction function) {
        double[] nv = new double[vec.length];
        for (int i = 0; i < vec.length; ++i)
            nv[i] = function.apply(vec[i]);
        return Vector.of(nv);
    }

    /**
     * Join vectors using function
     *
     * @param vecB     second vector
     * @param function joining function
     * @return new vector
     * @throws IllegalArgumentException when vectors are not of same length
     */
    public Vector join(Vector vecB, Operation function) {
        if (size() != vecB.size())
            throw new IllegalArgumentException("Vector sizes dose not match");
        double[] nv = new double[vec.length];
        for (int i = 0; i < vec.length; ++i)
            nv[i] = function.apply(vec[i], vecB.vec[i]);
        return Vector.of(nv);
    }

    public Vector plus(Vector vector) {
        return join(vector, (a, b) -> a + b);
    }

    public Vector minus(Vector vector) {
        return join(vector, (a, b) -> a - b);
    }

    public Vector times(double number) {
        return use(it -> number * it);
    }

    public Vector times(Vector vector) {
        return join(vector, (a, b) -> a * b);
    }

    public Vector divide(Vector vector) {
        return join(vector, (a, b) -> a / b);
    }

    public Vector divide(double number) {
        return use(it -> it / number);
    }

    /**
     * Sums vector
     *
     * @return sum
     */
    public double sum() {
        return DoubleStream.of(vec).sum();
    }

    /**
     * Return size of vector
     *
     * @return size of vector
     */
    public int size() {
        return vec.length;
    }

    /**
     * Creates vector from array
     *
     * @param vec double array
     * @return new vector
     */
    public static Vector of(double[] vec) {
        return new Vector(vec);
    }

    /**
     * Interface for mapping function
     */
    @FunctionalInterface
    public interface UseFunction {
        /**
         * Mapping function
         *
         * @param n number
         * @return number
         */
        Double apply(double n);
    }


    /**
     * Interface for operation on two vectors
     */
    @FunctionalInterface
    public interface Operation {
        /**
         * Mapping function
         *
         * @param a number from 1st vector
         * @param b number from 2nd vector
         * @return number
         */
        Double apply(double a, double b);
    }
}
