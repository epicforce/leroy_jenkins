package org.jenkins.plugins.leroy;

/**
 * Created by Dzmitry Bahdanovich on 27.06.14.
 */
public class LeroyException extends Exception {

    public LeroyException() {
    }

    public LeroyException(String message) {
        super(message);
    }

    public LeroyException(String message, Throwable cause) {
        super(message, cause);
    }

    public LeroyException(Throwable cause) {
        super(cause);
    }

    public LeroyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
