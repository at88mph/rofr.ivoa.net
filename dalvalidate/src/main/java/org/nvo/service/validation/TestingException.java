package org.nvo.service.validation;

/**
 * a general exception indicating that something went wrong while 
 * executing a test.
 */
public class TestingException extends Exception {

    public TestingException(String message) {
        super(message);
    }
}
