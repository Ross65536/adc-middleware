package pt.inesctec.adcauthmiddleware.utils;

public interface ThrowingProducer<R, E extends Throwable> {
  R get() throws E;
}
