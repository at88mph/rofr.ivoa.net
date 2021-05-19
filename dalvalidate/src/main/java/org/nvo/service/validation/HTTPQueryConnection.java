package org.nvo.service.validation;

import java.net.HttpURLConnection;

/**
 * an interface to a service stream running over HTTP
 */
public interface HTTPQueryConnection extends QueryConnection {

    /**
     * return the HttpURLConnection object.  It will already have been
     * opened.
     */
    public HttpURLConnection getHttpURLConnection();

    /**
     * return the HTTP response code or -1 if the code is not yet known
     */
    public int getResponseCode();

    /**
     * return the HTTP response code or null if the message was not received 
     * (yet).
     */
    public String getResponseMessage();

}
