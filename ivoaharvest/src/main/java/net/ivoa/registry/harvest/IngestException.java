package net.ivoa.registry.harvest;

/**
 * an exception indicating that an error occurred while attempting to ingest
 * harvested records.  
 */
public class IngestException extends HarvestingException {

    /**
     * create the exception, wrapping an uderlying excdeption
     * @param message    an explanation of the error
     * @param cause      the caught exception representing underlying cause of
     *                      the failure.  
     */
    public IngestException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * create the exception, wrapping an underlying exception
     * @param message    an explanation of the error
     * @param cause      the caught exception representing underlying cause of
     *                      the failure.  
     * @param goodCount  the number of successfully harvested records prior 
     *                      to this failure.  A negative number indicates that 
     *                      the number is not known.
     */
    public IngestException(String message, Throwable cause, int goodCount) {
        this(message, cause);
        reccount = goodCount;
    }

    /**
     * create the exception.
     * @param message    an explanation of the error
     * @param goodCount  the number of successfully harvested records prior 
     *                      to this failure.  A negative number indicates that 
     *                      the number is not known.
     */
    public IngestException(String message, int goodCount) {
        this(message, null, goodCount);
    }

    /**
     * create the exception.
     * @param message    an explanation of the error
     */
    public IngestException(String message) {
        this(message, null);
    }
}