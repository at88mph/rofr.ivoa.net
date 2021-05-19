package org.nvo.service.validation;

import org.w3c.dom.Element;

/**
 * an interface for executing a test query, evalutating its response,
 * and returning its response.  Different implementations will apply 
 * different strategies for communicating with the service.  The interface
 * implies that the Tester is attached to a particular TestQuery and 
 * Evaluator.
 */
public interface Tester {

    /**
     * apply the tests against the response from the attached TestQuery.  
     * The query will be invoke and managed internally.
     * @param addTo   the element to append the XML-encoded results into.
     * @return int    the number of test results written into addTo.  Note
     *                  that the actual number executed may have been higher.
     * @exception InterruptedException   if an interrupt signal was sent to 
     *                to this tester telling it to stop testing.
     * @exception TestingException   if any other non-recoverable error is 
     *                encountered while applying tests.
     */
    public int applyTests(Element addTo) 
         throws TestingException, InterruptedException;

    /**
     * return the TestQuery object that this tester will invoke
     */
    public TestQuery getTestQuery();

    /**
     * return the Evaluator object that this tester will use to evaluate
     * the query response.
     */
    public Evaluator getEvaluator();

}
