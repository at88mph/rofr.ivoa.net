package org.nvo.service.validation;

import org.w3c.dom.Element;

/**
 * a simple service tester that does not attempt any special handling
 * of the service response or test results.  Instead, it delegates the 
 * handling of a query to the Evaluator by calling its 
 * applyTests(TestQuery, Element); the evaluator must then make the 
 * connection to the service and apply the tests internally.  This is 
 * typically done by keeping all documents in memory.
 */
public class SimpleTester implements Tester {

    protected TestQuery tquery = null;
    protected Evaluator evaluator = null;

    /**
     * create the tester
     * @param tq   the TestQuery that will be invoked
     * @param te   the Evaluator that will be used to evaluate the results
     * @exception NullPointerException  if either input is null
     */
    public SimpleTester(TestQuery tq, Evaluator te) {
        this(tq, te, true);
    }

    /**
     * create the tester.  
     * @param tq   the TestQuery that will be invoked
     * @param te   the Evaluator that will be used to evaluate the results
     * @param ensureNonNull   if true, make sure the other inputs are non-null
     * @exception NullPointerException  if either input is null
     */
    protected SimpleTester(TestQuery tq, Evaluator te, boolean ensureNonNull) {
        tquery = tq;
        evaluator = te;
        if (ensureNonNull) {
            checkTestQuery();
            checkEvaluator();
        }
    }

    private void checkTestQuery() {
        if (tquery == null) 
            throw new NullPointerException("Null TestQuery provided");
    }

    private void checkEvaluator() {
        if (evaluator == null) 
            throw new NullPointerException("Null Evaluator provided");
    }

    /**
     * apply the tests against the response from the attached TestQuery.  
     * The query will be invoke and managed by the attached Evaluator 
     * through its applyTests(TestQuery, Element) method.  
     * @param addTo   the element to append the XML-encoded results into.
     * @return int    the number of test results written into addTo.  Note
     *                  that the actual number executed may have been higher.
     * @exception InterruptedException   if an interrupt signal was sent to 
     *                to this evaluator telling it to stop testing.
     * @exception TestingException   if any other non-recoverable error is 
     *                encountered while applying tests.
     */
    public int applyTests(Element addTo) 
         throws TestingException, InterruptedException 
    {
        return evaluator.applyTests(tquery, addTo);
    }

    /**
     * return the TestQuery object that this tester will invoke
     */
    public TestQuery getTestQuery() { return tquery; }

    /**
     * return the TestQuery object that this tester will invoke
     * @exception NullPointerException  if the input is null
     */
    public void setTestQuery(TestQuery tq) { 
        tquery = tq; 
        checkTestQuery();
    }

    /**
     * set the Evaluator object that this tester will use to evaluate
     * the query response.
     * @exception NullPointerException  if the input is null
     */
    public void setEvaluator(Evaluator te) { 
        evaluator = te; 
        checkEvaluator();
    }

    /**
     * return the Evaluator object that this tester is using to evaluate
     * the query response.
     * @exception NullPointerException  if the input is null
     */
    public Evaluator getEvaluator() { return evaluator; }
}
