package org.nvo.service.validation;

/**
 * an exception indicating a misconfiguration of the testing facility is 
 * preventing a successful test.  
 */
public class ConfigurationException extends TestingException {
    Exception wrapped = null;

    /**
     * create the exception with a given message
     */
    public ConfigurationException(String msg) { super(msg); }

    /**
     * create the exception with a given message
     * @param original   the original exception that was thrown indicating 
     *                      a configuration problem.
     */
    public ConfigurationException(Exception original) { 
        super(original.getMessage()); 
        wrapped = original;
    }

    /**
     * return the original exception that marked the problem
     */
    public Exception getOriginal() { return wrapped; }

}
