package org.nvo.service.validation;

import java.io.InputStream;
import java.io.IOException;

/**
 * an interface for managing the execution of a test query.  An implementation
 * is usually obtained from a TestQuery object.  
 */
public interface QueryConnection {

    /**
     * return the stream carrying the query response, blocking if necessary
     * until stream is ready.
     * @exception IOException   if an error occurs while opening a stream.
     * @exception InterruptedException   if the blocking was interrupted while
     *                            trying to retreive the stream.  This will 
     *                            occur if another thread calls shutdown().
     * @see org.nvo.service.validation.HTTPServerException
     */
    public InputStream getStream() throws IOException, InterruptedException;

    /**
     * return true if an InputStream object is ready for reading from
     */
    public boolean isStreamReady();

    /**
     * wait for the response stream to be ready.
     * @param timeout   maximum time to wait
     * @return boolean  true if the stream is now ready or false if the timeout
     *                    expired before the stream became available.
     * @exception IOException            if an IOException occurs before the 
     *                                      timeout expires.
     * @exception InterruptedException   if the current thread is interrupted
     */
    public boolean waitUntilReady(long timeout) 
         throws IOException, InterruptedException; 

    /**
     * attempt to shutdown the connection.  This would be invoked if 
     * getting the response is taking too long.
     */
    public void shutdown() throws IOException;

}
