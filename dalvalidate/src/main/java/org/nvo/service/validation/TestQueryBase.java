package org.nvo.service.validation;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Iterator;
import java.io.IOException;

/**
 * a base class for TestQuery implementations.  This base class provides 
 * implementations of support capabilities.  In other words, this implementation 
 * does everything a TestQuery needs to do except actually invoking the 
 * test query; subclasses implement the invoke method.  
 */
public abstract class TestQueryBase implements TestQuery, Cloneable {

    protected Properties props = new Properties();
    protected ResultTypes rtypes = new ResultTypes(4);
    protected String name = null;
    protected String type = null;
    protected String desc = null;
    protected HashSet ignore = null;
    protected long timeout = defaultTimeout;

    /**
     * the default timeout time for TestQuery objects.  By default, this 
     * is set to 10 minutes, but it can be overridden.
     */
    public static long defaultTimeout = 600000;  // default: 10 minutes

    /**
     * create the default implementation.  
     */
    public TestQueryBase() { this(null, null); }

    /**
     * create the base implementation.  If either input is null, it 
     * will be set with a default.
     * @param resTypes   the ResultTypes object to start with
     * @param evalProps  the evalutation properties to start with.
     */
    public TestQueryBase(ResultTypes resTypes, Properties evalProps) {
        props = evalProps;
        rtypes = resTypes;
        if (rtypes == null) rtypes = new ResultTypes(4);
        if (props == null) props = new Properties();
    }

    /**
     * create the base implementation.  If any input is null, it 
     * will be set with a default.
     * @param name       the name of the test query
     * @param type       the type of test query
     * @param desc       a description of the test query
     * @param resTypes   the ResultTypes object to start with
     * @param evalProps  the evalutation properties to start with.
     */
    public TestQueryBase(String name, String type, String desc,
                         ResultTypes resTypes, Properties evalProps) 
    {
        this(resTypes, evalProps);
        this.name = name;
        this.type = type;
        this.desc = desc;
    }

    /**
     * invoke the query and return the stream carrying the response
     * @exception IOException   if an error occurs while opening a stream.
     */
    public abstract QueryConnection invoke() throws IOException;

    /**
     * return the name of the query.  This is usually unique among 
     * a set of queries and is used only as an identifier.
     */
    public String getName() { return name; }

    /**
     * set the name of the query
     */
    public void setName(String name) { this.name = name; }

    /**
     * return the query type.  This name identifies a class of queries
     * that should all be evaluated in a similar way.
     */
    public String getType() { return type; }

    /**
     * set the type of the query
     */
    public void setType(String type) { this.type = type; }

    /**
     * return a short description of the query for display purposes or 
     * null if one is not available.  
     */
    public String getDescription() { return desc; }

    /**
     * set the name of the query
     */
    public void setDescription(String desc) { this.desc = desc; }

    /** 
     * configure this test query by setting a bunch of the basic info
     * @param name       the name of the test query.  If null, the value
     *                     is unchanged.
     * @param type       the type of test query.  If null, the value
     *                     is unchanged.
     * @param desc       a description of the test query.  If null, the value
     *                     is unchanged.
     * @param timeout    the timeout for this query.  If <= 0, the value will
     *                     be unchanged.
     */
    public void configure(String name, String type, String desc, 
                          long timeout, String ignore)
    {
        if (name != null) setName(name);
        if (type != null) setType(type);
        if (desc != null) setDescription(desc);
        if (timeout > 0) setTimeout(timeout);
    }

    /**
     * return a bit field of the result types that should be returned
     * when the test query is evaluated.
     * @see org.nvo.service.validation.ResultTypes
     */
    public int getResultTypes() { return rtypes.getTypes(); }

    /**
     * set the result types
     * @param types   the types OR-ed together
     */
    public void setResultTypes(int types) { rtypes.setTypes(types); }

    /**
     * add the result types to the current set
     * @param types   the types OR-ed together to add
     */
    public void addResultTypes(int types) { rtypes.addTypes(types); }

    /**
     * return the desired result types as a space-delimited concatonation 
     * of the result type tokens 
     * @see org.nvo.service.validation.ResultTypes
     */
    public String getResultTokens() { return rtypes.getTypeTokens(); }

    /**
     * return a ResultTypes object for updating the types handled by 
     * this query
     */
    public ResultTypes getResultTypesMgr() { return rtypes; }

    /**
     * return the names of all ignorable tests in a space-delimited 
     * concatonated String.
     */
    public String getIgnorableTests() {
        if (ignore == null) return "";

        StringBuffer out = new StringBuffer();
        for(Iterator iter = ignore.iterator(); iter.hasNext();) {
            out.append((String) iter.next());
            if (iter.hasNext()) out.append(' ');
        }
        return out.toString();
    }

    /**
     * add a set of test names that can be ignored or skipped when 
     * executing this test query
     * @param testnames   a space-delimited concatonation of names
     */
    public void addIgnorableTests(String testnames) {
        if (testnames == null || testnames.trim().length() == 0) 
            return;

        if (ignore == null) ignore = new HashSet();
        StringTokenizer st = new StringTokenizer(testnames);
        while (st.hasMoreTokens()) {
            ignore.add(st.nextToken());
        }
    }

    /**
     * reset the set of test names that can be ignored or skipped when 
     * executing this test query.  All previously registered names will 
     * be removed.  
     * @param testnames   a space-delimited concatonation of names.  If null
     *                       or empty, the set will be empty
     */
    public void setIgnorableTests(String testnames) {
        ignore = null;
        addIgnorableTests(testnames);
    }

    /**
     * return the recommended timeout time in milliseconds for 
     * a response.
     */
    public long getTimeout() { return timeout; }

    /**
     * return the recommended timeout time in milliseconds for 
     * a response.
     * @param millis  the time in milliseconds.  If <= 0, the timeout
     *                   will be set to the current default value;
     */
    public void setTimeout(long millis) { 
        if (millis <= 0) millis = defaultTimeout;
        timeout = millis; 
    }

    /**
     * return the evaluation properties.  These tend to be specific
     * to the type of test and the evaluation method.  
     * @return Properties   the named properties
     * @see org.nvo.service.validation.HTTPGetTestQuery
     */
    public Properties getEvaluationProperties() { return props; }

    /**
     * set an evaluation property.
     * @param name   the name of the property to set
     * @param value  the value of the property to set
     */
    public void setEvalProperty(String name, String value) { 
        props.setProperty(name, value);
    }

    /**
     * create a sensible clone of this object
     */
    public Object clone() {
        try {
            TestQueryBase out = (TestQueryBase) super.clone();
            out.rtypes = (ResultTypes) rtypes.clone();
            if (ignore != null) {
                out.ignore = (HashSet) ignore.clone();
            }
            return out;
        }
        catch (CloneNotSupportedException ex) {
           throw new InternalError("programmer clone error: "+ex.getMessage());
        }
            
    }
}
