package org.nvo.service.validation.webapp;

/**
 * an exception indicating an error while fulfilling a validation request.
 */
public class ValidationException extends Exception {
    protected Exception wrapped;

    /**
     * create the exception 
     */
    public ValidationException() { super("unknown validation request error"); }

    /**
     * create the exception 
     */
    public ValidationException(String message) { super(message); }

    /**
     * create the exception, wrapping another exception
     */
    public ValidationException(Exception ex, String message) { 
        this(message); 
        wrapped = ex;
    }

    /**
     * create the exception, wrapping another exception
     */
    public ValidationException(Exception ex) { 
        this(ex.getMessage()); 
        wrapped = ex;
    }

    /**
     * return the wrapped exception
     */
    public Exception getWrapped() { return wrapped; }
}
