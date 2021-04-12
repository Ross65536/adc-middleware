package pt.inesctec.adcauthmiddleware.http;

/**
 * Thrown on the synchronization endpoint on user errors.
 */
public class SyncException extends Exception {
    public SyncException(String s) {
        super(s);
    }
}
