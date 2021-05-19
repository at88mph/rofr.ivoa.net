package org.nvo.service.validation;

import java.util.Properties;
import java.net.URL;

/**
 * a TestQuery that is invoked via an HTTP Get Query characterized by 
 * a URL
 */
public class URLTestQuery extends TestQueryBase implements HTTPTestQuery {
    protected URL url = null;

    /**
     * create a fully initialized test query
     * @param name       the name of the test query
     * @param type       the type of test query
     * @param desc       a description of the test query
     */
    public URLTestQuery(URL queryURL, String name, String type, String desc) {
        this(queryURL, name, type, desc, null, null);
    }

    /**
     * create a fully initialized test query
     * @param queryURL   the URL to use to invoke the query
     * @param name       the name of the test query
     * @param type       the type of test query
     * @param desc       a description of the test query
     * @param resTypes   the ResultTypes object to start with
     * @param evalProps  the evalutation properties to start with.
     */
    public URLTestQuery(URL queryURL, String name, String type, String desc,
                        ResultTypes resTypes, Properties evalProps) 
    { 
        super(name, type, desc, resTypes, evalProps);
        url = queryURL;
    }

    /**
     * create a test query, initializing all info but the URL
     * @param name       the name of the test query
     * @param type       the type of test query
     * @param desc       a description of the test query
     * @param resTypes   the ResultTypes object to start with
     * @param evalProps  the evalutation properties to start with.
     */
    protected URLTestQuery(String name, String type, String desc,
                           ResultTypes resTypes, Properties evalProps) 
    { 
        super(name, type, desc, resTypes, evalProps);
    }

    /**
     * create a test query, with minimal initialization
     * @param resTypes   the ResultTypes object to start with
     * @param evalProps  the evalutation properties to start with.
     */
    protected URLTestQuery(ResultTypes resTypes, Properties evalProps) 
    { 
        super(resTypes, evalProps);
    }

    /**
     * return the URL that is used to invoke the test
     */
    public URL getURL() { return url; }

    /**
     * invoke the service and return the stream carrying the response.  
     * This will return an HTTPQueryConnection object.
     */
    public QueryConnection invoke() { return invokeOverHTTP(); }

    /**
     * invoke the query and return the stream carrying the response
     * @exception IOException   if an error occurs while opening a stream.
     */
    public HTTPQueryConnection invokeOverHTTP() {
        return new HTTPGetQueryConnection(url);
    }

}
