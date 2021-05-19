package org.nvo.service.validation;

import net.ivoa.util.Configuration;

import java.util.Properties;
import java.util.List;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * a TestQuery that is invoked via an HTTP Get Query characterized by 
 * a base URL and a set of arguments.
 *
 * This class also provides some static methods for getting test query 
 * information out of Configuration.   
 */
public class HTTPGetTestQuery extends URLTestQuery {
    protected String baseURL = null;
    protected String args = "";

    /**
     * create a fully specified test query
     * @param baseurl    the base URL.  This may be modified to append a 
     *                      question mark or ampersand as needed.  
     * @param args       the service arguments as a URL argument string.  This 
     *                     value will be appended onto the (modified) base
     *                     URL to form the query.  Can be null.
     * @param name       the name of the test query
     * @param type       the type of test query
     * @param desc       a description of the test query
     * @param resTypes   the ResultTypes object to start with
     * @param evalProps  the evalutation properties to start with.
     * @exception MalformedURLException  if baseURL and args does not 
     *                   form a legal URL.  
     */
    public HTTPGetTestQuery(String baseurl, String args, 
                            String name, String type, String desc,
                            ResultTypes resTypes, Properties evalProps) 
         throws MalformedURLException
    { 
        super(name, type, desc, resTypes, evalProps);
        baseURL = baseurl;
        setURLArgs(args);
    }

    /**
     * create a fully specified test query.  To get this instance into 
     * a legal state, setURLArgs() must be called.  
     * @param baseurl    the base URL.  This may be modified to append a 
     *                      question mark or ampersand as needed.  
     * @param name       the name of the test query
     * @param type       the type of test query
     * @param desc       a description of the test query
     * @param evalProps  the evalutation properties to start with.
     */
    public HTTPGetTestQuery(String baseurl, String name, String type, 
                            String desc, Properties evalProps) 
    { 
        super(name, type, desc, null, evalProps);
        baseURL = baseurl;
    }

    /**
     * create a fully specified test query.  To get this instance into 
     * a legal state, setURLArgs() must be called.  getName() and 
     * getDescription() should also be called.  
     * @param baseurl    the base URL.  This may be modified to append a 
     *                      question mark or ampersand as needed.  
     * @param evalProps  the evalutation properties to start with.
     */
    public HTTPGetTestQuery(String baseurl, Properties evalProps) 
    { 
        super(null, null, null, null, evalProps);
        baseURL = baseurl;
    }

    /**
     * create a fully specified test query
     * @param baseurl     the base URL.  This may be modified to append a 
     *                      question mark or ampersand as needed.  
     * @param tq          another test query to copy from
     * @param shareProps  if false, the properties from tq will be cloned.
     */
    public HTTPGetTestQuery(String baseurl, TestQueryBase tq, 
                            boolean shareProps) 
    { 
        super(tq.getName(), tq.getType(), tq.getDescription(), 
              (ResultTypes) tq.getResultTypesMgr().clone(), 
              (shareProps) ? tq.getEvaluationProperties() 
                          : (Properties) tq.getEvaluationProperties().clone());
        baseURL = baseurl;
        setTimeout(tq.getTimeout());
        setIgnorableTests(tq.getIgnorableTests());
    }

    /**
     * return the arguments to the HTTP Get query
     */
    public String getURLArgs() { return args; }

    /** 
     * set the arguments to the HTTP Get query
     */
    public void setURLArgs(String arguments) 
         throws MalformedURLException
    { 
        url = makeURL(arguments);
        args = arguments;
    }

    /**
     * set the data for this query 
     * @param qd     the QueryData object containing the query name, type,
     *                 description, and (most importantly) the arguments.
     *                 Any null data will be ignored, leaving previous values
     *                 in place.  
     */
    public void setQueryData(QueryData qd) throws MalformedURLException {
        setName(qd.name);
        setType(qd.type);
        setDescription(qd.desc);
        setURLArgs(qd.args);
        if (qd.restypes > 0) setResultTypes(qd.restypes);
    }

    /**
     * create a URL from the base URL and the given arguments
     * @param argstr   the argument string.  If null, only the baseURL 
     *                    will be returned.
     */
    protected URL makeURL(String argstr) throws MalformedURLException {
        if (argstr == null) argstr = "";
        return new URL(baseURL + argstr);
    }

    /**
     * return the base URL to the service.  This URL will end in either 
     * a question mark (?) or an ampersand (&).  
     */
    public String getBaseURL() { return baseURL; }

    /**
     * extract a testQuery sub-configuration from the given Configuration
     * @param config  the Configuration to extract from.
     * @param name    select the one with the given name.  If null, the 
     *                 first occurance will be returned.
     */
    public static Configuration getTestQueryConfig(Configuration config, 
                                                   String name) 
    {
        String attname = (name != null) ? "name" : null;
        return config.getConfiguration("testQuery", attname, name);
    }

    /**
     * extract a base URL from a testQuery configuration.  This method
     * assumes a Configuration as returned by getTestQueryConfig().  
     * @param config  the Configuration to extract from.
     * @param name    select the base URL with the given name.  If null, the 
     *                 first occurance will be returned.
     */
    public static String getBaseURL(Configuration config, String name) {
        String attname = (name != null) ? "name" : null;
        Configuration c =  config.getConfiguration("baseURL", attname, name);
        return ((c != null) ? c.getParameter("") : null);
    }

    /**
     * extract a test query with a given name from a testQuery configuration.  
     * If the name is null, return the query with the name "default".  If 
     * this does not exist, just return the first one found or null if none
     * are available.  
     * @param config  the Configuration to extract from.  This Configuration 
     *                   is expected to have one or more top-level elements 
     *                   called "testQuery".
     * @param name    the query name to look for
     */
    public static QueryData getQueryData(Configuration config, String name) {
        Configuration[] c = config.getBlocks("query");
        Configuration out = null;
        String defstr = "default";
        String value = null;
        for(int i=0; i < c.length; i++) {
            value = c[i].getParameter("@name");
            if (defstr.equals(value)) 
                out = c[i];
            if (name == null) {
                if (out != null) break;
            }
            else if (name.equals(value)) {
                out = c[i];
                break;
            }
        }

        if (out == null && name == null && c.length > 0) out = c[0];

        if (out == null) return null;

        return new QueryData(out.getParameter("@name"), 
                             out.getParameter("@type"),
                             out.getParameter(""),
                             out.getParameter("@description"));
    }

    /**
     * find all the test queries in a give testQuery configuration and
     * add them as QueryData objects to the given list. 
     * @param config  the Configuration to extract from.  This Configuration 
     *                   is expected to have one or more top-level elements 
     *                   called "query".
     * @param queryList    a List object to add test queries to.  
     * @return int    the number of queries added.  
     * @exception IllegalArgumentException  if baseURL is null and no baseURL
     *                   is found in the configuration.
     */
    public static int addAllQueryData(Configuration config, List queryList) {

        Configuration[] c = config.getBlocks("query");

        for(int i=0; i < c.length; i++) {
            queryList.add(new QueryData(c[i].getParameter("@name"), 
                                        c[i].getParameter("@type"),
                                        c[i].getParameter(""),
                                        c[i].getParameter("@description")));
        }

        return c.length;
    }

    /**
     * a container for test query information.
     */
    public static class QueryData {
        /** the configured name for the query */
        public String name = null;

        /** the expected query content type */
        public String type = null;

        /** the arguments of the query, to be appended to a base URL */
        public String args = null;

        /** the description of the query */
        public String desc = null;

        /** the OR-ed combination of desired result type values */
        public int restypes = 0;

        public QueryData() { }
        public QueryData(String name, String type, String args, String desc) {
            this(name, type, args, desc, -1);
        }
        public QueryData(String name, String type, String args, String desc,
                         int resultTypes) 
        {
            this.name = name; 
            this.type = type;
            this.args = args;
            this.desc = desc;
            if (resultTypes >= 0) restypes = resultTypes;
        }
    }   
}
