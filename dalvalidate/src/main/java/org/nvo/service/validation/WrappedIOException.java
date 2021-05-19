package org.nvo.service.validation;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * an IO exception wrapping another exception.  This is used to transport 
 * an original IOException out of the thread where it was thrown while still
 * preserving the stacktrace.
 */
public class WrappedIOException extends IOException {

    IOException inner = null;

    /**
     * create a "wrapped" Exception with no parent exception
     */
    public WrappedIOException() { super(); }

    /**
     * create a wrapped Exception
     */
    public WrappedIOException(IOException ex) { 
        this(getName(ex) + ": " + ex.getMessage(), ex);
    }

    /**
     * create a wrapped Exception
     */
    public WrappedIOException(String msg, IOException ex) { 
        super(msg);
        inner = ex;
    }

    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        s.print("Original Exception Trace: ");
        inner.printStackTrace(s);
    }

    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
        s.print("Original Exception Trace: ");
        inner.printStackTrace(s);
    }

    static String getName(IOException ex) {
        String out = ex.getClass().getName();
        if (out.startsWith("java.io.")) out = out.substring(8);
        return out;
    }

    /**
     * return the wrapped exception
     */
    public IOException getException() { return inner; }

}
