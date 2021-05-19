package net.ivoa.registry.harvest;

/**
 * a container for OAI resumption token data.  
 * 
 * <p>
 * In the OAI protocol, a resumption token is used to indicate if additional
 * records can be received with additional calls to ListRecords.  More is 
 * available if a token value was provided.  This can be passed to the next
 * retrieval call by setting its "resumptionToken" argument to this value.  
 */
public class ResumptionToken {

    String tok = null;
    String date = null;
    int size = -1;
    int curs = -1;

    /**
     * create the token container
     */
    public ResumptionToken(String token, String expirationDate, 
                           int completeSize, int cursor) 
    {
        token = tok;
        date = expirationDate;
        size = completeSize;
        curs = cursor;
    }

    /**
     * create the token container
     */
    public ResumptionToken(String token) {
        this(token, null, -1, -1);
    }

    /**
     * create the token container
     */
    public ResumptionToken(String token, String expirationDate, 
                           String completeSize, String cursor) 
        throws NumberFormatException
    {
        this(token, expirationDate, 
             (completeSize == null) ? -1 : Integer.parseInt(completeSize), 
             (cursor == null)       ? -1 : Integer.parseInt(cursor));
    }    

    /**
     * return true if, according to this object, more records are available
     */
    public boolean moreRecords() { return (tok != null && tok.length() > 0); }

    /**
     * return the value of the token string.  Null is returned if no
     * further data is available (and resumption is not necessary).
     */
    public String getTokenValue() { return tok; }

    /**
     * return the value of the token string or an empty string if none is
     * set.
     */
    public String toString() { return (tok == null) ? "" : tok; }

    /**
     * return the expiration date (as a String) or null if none is specified.
     */
    public String getExpirationDate() { return date; }

    /**
     * return the (estimated) total size of the matching list or -1 if not
     * specified.
     */
    public int getCompleteListSize() { return size; }

    /**
     * return the number of records that have been returned so far, or -1 if 
     * not specified.
     */
    public int getCursor() { return curs; } 
}