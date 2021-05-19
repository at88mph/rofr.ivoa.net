package net.ivoa.registry.validate;

import org.nvo.service.validation.TestingException;

/**
 * an exception indicating that something went wrong in the execution of 
 * the OAI Explorer tool.
 * @see OAIExplorerTestQuery
 * @see OAIEvaluator
 */
public class OAIExplorerException extends TestingException { 

    /**
     * create the exception that explains the problem running the OAI Explorer 
     * tool
     */
    public OAIExplorerException(String message) { super(message); }
}