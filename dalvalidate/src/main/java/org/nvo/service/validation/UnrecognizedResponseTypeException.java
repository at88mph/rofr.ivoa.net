package org.nvo.service.validation;

/**
 * an exception indicating that the response from a service was of an 
 * unrecognized type and there for could not be tested. 
 */
public class UnrecognizedResponseTypeException extends TestingException {

    /**
     * create the exception with a given message
     */
    public UnrecognizedResponseTypeException(String msg) { super(msg); }

}
