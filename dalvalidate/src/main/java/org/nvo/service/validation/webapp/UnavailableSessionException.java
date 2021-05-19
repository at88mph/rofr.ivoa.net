package org.nvo.service.validation.webapp;

/**
 * an exception indicating that the requested session is in an unavailable 
 * state.  
 */
public class UnavailableSessionException extends ValidationException {

    /**
     * create the exception 
     */
    public UnavailableSessionException() { 
        super("Session is unavailable for unknown reasons"); 
    }

    /**
     * create the exception 
     */
    public UnavailableSessionException(String message) { super(message); }

    /**
     * create the exception, wrapping another exception
     */
    public UnavailableSessionException(Exception ex, String message) { 
        super(ex, message); 
    }

    /**
     * create the exception, wrapping another exception
     */
    public UnavailableSessionException(Exception ex) { 
        super(ex);
    }
}
