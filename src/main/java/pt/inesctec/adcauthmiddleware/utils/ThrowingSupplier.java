package pt.inesctec.adcauthmiddleware.utils;

/**
 * Like {@link java.util.function.Supplier} but can throw exception.
 *
 * @param <R> return type
 * @param <E> exception type
 */
public interface ThrowingSupplier<R, E extends Throwable> {
  R get() throws E;
}
