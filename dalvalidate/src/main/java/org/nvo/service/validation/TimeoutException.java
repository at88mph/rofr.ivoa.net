package org.nvo.service.validation;

/**
 * an exception indicating that a timeout period was exceeded before getting
 * a proper response from a service.
 */
public class TimeoutException extends TestingException {
    long timeout = 0L;

    /**
     * create the exception with a given message
     */
    public TimeoutException(String message) {
        super(message);
    }

    /**
     * create the exception with a default message based on the elapsed time
     * @param timeout   the timeout period that was exceeded in milliseconds
     */
    public TimeoutException(long timeout) {
        super("Timeout limit reached after " + timeout + " milliseconds");
        this.timeout = timeout;
    }

    /**
     * create the exception with a given message
     * @param timeout   the timeout period that was exceeded in milliseconds
     */
    public TimeoutException(long timeout, String message) {
        this(message);
        this.timeout = timeout;
    }

    /** 
     * return the timeout period that was exceeded or 0 if it is not known
     * @return long   the timeout period that was exceeded in milliseconds
     */
    public long getTimeout() { return timeout; }
}
