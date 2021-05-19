package org.nvo.service.validation;

import java.util.Properties;

/**
 * a test query object that cannot actually invoke a test but is rather 
 * intended to serve as a template for real queries.  As a thin wrapper 
 * around {@link TestQueryBase}, this class can serve as a container for 
 * static data that is used to apply a number of common tests.  Since this 
 * class can't invoke a test, the {@link #invoke()} method always throws an 
 * exception.
 */
public class TemplateTestQuery extends TestQueryBase {

    /**
     * create the default implementation.  
     */
    public TemplateTestQuery() { super(); }

    /**
     * create the base implementation.  If either input is null, it 
     * will be set with a default.
     * @param resTypes   the ResultTypes object to start with
     * @param evalProps  the evalutation properties to start with.
     */
    public TemplateTestQuery(ResultTypes resTypes, Properties evalProps) {
        super(resTypes, evalProps);
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
    public TemplateTestQuery(String name, String type, String desc,
                         ResultTypes resTypes, Properties evalProps) 
    {
        super(name, type, desc, resTypes, evalProps);
    }

    /**
     * invoke the query and return the stream carrying the response.  This
     * implementation always throws an exception.
     * @exception InternalError always.
     */
    public QueryConnection invoke() {
        throw new InternalError("invoked a non-functional TemplateTestQuery");
    }
}
