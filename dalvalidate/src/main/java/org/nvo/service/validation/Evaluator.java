package org.nvo.service.validation;

import java.io.InputStream;
import org.w3c.dom.Element;

/**
 * an interface for evaluating the response from a TestQuery via a 
 * series of tests and returning the results.  The results will be 
 * encoded in XML and inserted into a given document.
 */
public interface Evaluator {

    /**
     * apply the tests against the response from a TestQuery.  The query
     * will be invoke and managed internally via the given TestQuery, 
     * heeding its advice as applicable.
     * @param tq      the test to invoke
     * @param addTo   the element to append the XML-encoded results into.
     * @return int    the number of test results written into addTo.  Note
     *                  that the actual number executed may have been higher.
     * @exception InterruptedException   if an interrupt signal was sent to 
     *                to this evaluator telling it to stop testing.
     * @exception TestingException   if any other non-recoverable error is 
     *                encountered while applying tests.
     */
    public int applyTests(TestQuery tq, Element addTo) 
         throws TestingException, InterruptedException;

    /**
     * apply the tests against the response from a TestQuery.  This version 
     * assumes the query has already been invoke and the response is now
     * available on the given input stream.  The tests will be applied, 
     * heeding the advice of the given TestQuery as applicable.  
     * @param response  the response stream that results from invoking 
     *                     the TestQuery, tq.
     * @param tq        the test to invoke
     * @param addTo     the element to append the XML-encoded results into.
     * @return int    the number of test results written into addTo.  Note
     *                  that the actual number executed may have been higher.
     * @exception InterruptedException   if an interrupt signal was sent to 
     *                to this evaluator telling it to stop testing.
     * @exception TestingException   if any other non-recoverable error is 
     *                encountered while applying tests.
     */
    public int applyTests(InputStream response, TestQuery tq, Element addTo)
         throws TestingException, InterruptedException;

}
