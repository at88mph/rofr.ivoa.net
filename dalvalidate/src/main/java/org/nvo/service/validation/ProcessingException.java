package org.nvo.service.validation;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * an exception indicating an IOException occurred while accessing the
 * service or its cached response.
 */
public class ProcessingException extends TestingException {
    Exception wrapped = null;

    /**
     * create the exception with a given message
     */
    public ProcessingException(String msg) { super(msg); }

    /**
     * create the exception with a given message
     * @param original   the original exception that was thrown indicating 
     *                      a configuration problem.
     */
    public ProcessingException(Exception original) { 
        super(original.getMessage()); 
        wrapped = original;
    }

    /**
     * return the original exception that marked the problem
     */
    public Exception getOriginal() { return wrapped; }

    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        if (wrapped != null) {
            s.print("Original Exception Trace: ");
            wrapped.printStackTrace(s);
        }
    }

    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
        if (wrapped != null) {
            s.print("Original Exception Trace: ");
            wrapped.printStackTrace(s);
        }
    }

}
