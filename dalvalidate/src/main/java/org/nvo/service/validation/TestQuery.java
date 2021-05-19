package org.nvo.service.validation;

import java.util.Properties;
import java.io.IOException;

/**
 * an interface to a representation of a test query.  
 * A test query, when invoked, will return a stream of data.  This interface 
 * also provides advice on how to evaluate the response and represent the 
 * results of that evaluation.
 */
public interface TestQuery {

    /**
     * invoke the query and return the stream carrying the response
     * @exception IOException   if an error occurs while opening a stream.
     */
    public QueryConnection invoke() throws IOException;

    /**
     * return the name of the query.  This is usually unique among 
     * a set of queries and is used only as an identifier.
     */
    public String getName();

    /**
     * return the query type.  This name identifies a class of queries
     * that should all be evaluated in a similar way.
     */
    public String getType();

    /**
     * return a short description of the query for display purposes or 
     * null if one is not available.  
     */
    public String getDescription();

    /**
     * return a bit field of the result types that should be returned
     * when the test query is evaluated.
     * @see org.nvo.service.validation.ResultTypes
     */
    public int getResultTypes();

    /**
     * return the desired result types as a space-delimited concatonation 
     * of the result type tokens 
     * @see org.nvo.service.validation.ResultTypes
     */
    public String getResultTokens();

    /**
     * return the names of all ignorable tests in a space-delimited 
     * concatonated String.
     */
    public String getIgnorableTests();

    /**
     * return the recommended timeout time in milliseconds for 
     * a response.
     */
    public long getTimeout();

    /**
     * return the evaluation properties.  These tend to be specific
     * to the type of test and the evaluation method.  
     * @return Properties   the named properties
     * @see org.nvo.service.validation.HTTPGetTestQuery
     */
    public Properties getEvaluationProperties();
}
