package net.ivoa.registry.harvest;

/**
 * an exception indicating an error occurred while harvesting from a registry.
 * It is intended that this exception reflect issues that originate with 
 * registry harvesting service and its output, rather than local problems 
 * processing the output.  The latter should be handled via an IOException.  
 */
public class HarvestingException extends Exception {

    int reccount = -1;
    String request = null;

    /**
     * create the exception, wrapping an underlying exception
     * @param message    an explanation of the error
     * @param cause      the caught exception representing underlying cause of
     *                      the failure.  
     */
    public HarvestingException(String message, Throwable cause) {
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
    public HarvestingException(String message, Throwable cause, int goodCount) {
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
    public HarvestingException(String message, int goodCount) {
        this(message, null, goodCount);
    }

    /**
     * create the exception.
     * @param message    an explanation of the error
     */
    public HarvestingException(String message) {
        this(message, null);
    }

    /**
     * return the number of successfully harvested records prior to the 
     * failure.  
     * @return int  the count or a negative number if the number is not known.
     */
    public int getCompletedRecordCount() {  return reccount;  }

    /**
     * set the number of successfully harvested records prior to the 
     * failure.  
     */
    public void setCompletedRecordCount(int count) {  reccount = count;  }

    /**
     * return the request URL that produced the error.  Null is returned 
     * the request is unknown.
     */
    public String getRequestURL() { return request; }

    /**
     * return the request URL that produced the error.  Null is returned 
     * the request is unknown.
     */
    public void setRequestURL(String requestURL) { request = requestURL; }
}