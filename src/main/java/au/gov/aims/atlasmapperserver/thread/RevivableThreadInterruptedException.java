package au.gov.aims.atlasmapperserver.thread;

/**
 * Exception thrown when the user click the stop button.
 * It extends Throwable instead of Exception therefore it's not catchable with "catch (Exception)".
 * It's harder to catch therefore it's more likely to bubble up and cancel the the thread execution.
 * Inspired from java.lang.Exception
 */
public class RevivableThreadInterruptedException extends Throwable {

    public RevivableThreadInterruptedException() {
        super();
    }

    public RevivableThreadInterruptedException(String message) {
        super(message);
    }

    public RevivableThreadInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RevivableThreadInterruptedException(Throwable cause) {
        super(cause);
    }
}
