package pt.inesctec.adcauthmiddleware.utils;

/**
 * Like {@link java.util.function.Function} but can throw exception.
 *
 * @param <T> argument type
 * @param <R> return type
 * @param <E> exception type
 */
public interface ThrowingFunction<T, R, E extends Throwable> {
    R apply(T t) throws E;
}
