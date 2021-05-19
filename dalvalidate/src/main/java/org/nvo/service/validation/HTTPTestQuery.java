package org.nvo.service.validation;

/**
 * an interface to a representation of a test query that operates over
 * HTTP.  This provides a version of invoke() that returns a 
 * HTTPQueryConnection as a coding convenience.  
 */
public interface HTTPTestQuery extends TestQuery {

    /**
     * invoke the query and return the stream carrying the response
     * @exception IOException   if an error occurs while opening a stream.
     */
    public HTTPQueryConnection invokeOverHTTP();

}



