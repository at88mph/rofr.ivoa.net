package org.nvo.service.validation.webapp;

/**
 * an exception indicating that although the client's request was legal,
 * the server encounter an internal error due to either a configuration 
 * or state problem.  An HTTP servlet would typically respond with 500 message
 * upon catching this exception.  
 */
public class InternalServerException extends ValidationException {

    /**
     * create the exception 
     */
    public InternalServerException() { 
        super("Requested operation failed due to internal server problem"); 
    }

    /**
     * create the exception 
     */
    public InternalServerException(String message) { super(message); }

    /**
     * create the exception, wrapping another exception
     */
    public InternalServerException(Exception ex, String message) { 
        super(ex, message); 
    }

    /**
     * create the exception, wrapping another exception
     */
    public InternalServerException(Exception ex) { 
        super(ex);
    }
}
