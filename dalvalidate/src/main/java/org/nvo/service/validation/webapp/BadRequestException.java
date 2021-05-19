package org.nvo.service.validation.webapp;

/**
 * an exception indicating that the client's request was ill-formed in 
 * some way.  An HTTP servlet would typically respond with 400 message
 * upon catching this exception.  
 */
public class BadRequestException extends ValidationException {

    /**
     * create the exception 
     */
    public BadRequestException() { 
        super("Illegal request"); 
    }

    /**
     * create the exception 
     */
    public BadRequestException(String message) { super(message); }

    /**
     * create the exception, wrapping another exception
     */
    public BadRequestException(Exception ex, String message) { 
        super(ex, message); 
    }

    /**
     * create the exception, wrapping another exception
     */
    public BadRequestException(Exception ex) { 
        super(ex);
    }
}
