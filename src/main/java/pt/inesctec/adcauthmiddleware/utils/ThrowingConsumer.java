package pt.inesctec.adcauthmiddleware.utils;

/**
 * Like {@link java.util.function.Consumer} but can throw exception.
 *
 * @param <T> argument type
 * @param <E> exception type
 */
public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T t) throws E;
}
