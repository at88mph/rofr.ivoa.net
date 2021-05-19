package org.nvo.service.validation;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder; 
import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * an interface for evaluating the XML response from a TestQuery via a 
 * series of tests and returning the results.  The results will be 
 * encoded in XML and inserted into a given document.
 */
public interface XMLResponseEvaluator extends Evaluator {

    /**
     * apply the tests against the response from a TestQuery.  With this 
     * method it is assumed that the query has already been submitted and the 
     * response has been parsed into the given DOM Document.  Any errors that 
     * were encountered can be sent in as a ParsingErrors object.  
     * will be invoke and managed internally via the given TestQuery, 
     * heeding its advice as applicable.
     * @param response  the response data that results from invoking 
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
    public int applyTests(Document response, TestQuery tq, Element addTo)
         throws TestingException, InterruptedException;

    /**
     * if applicable, register an ErrorHandler with the parser.  If the 
     * implementation does not want to provide any special handling of parser 
     * errors, it can return null.  Capturing parsing errors is particularly 
     * useful when using a validating parser.
     * @param db       the DOM parser to give the ErrorHandler to
     * @param query    the TestQuery being handled.  The ErrorHandler that 
     *                     gets applied may depend on the query being sent.  
     * @return ParsingErrors  the ErrorHandler object that was set.
     */
    public ParsingErrors applyParsingErrorHandler(DocumentBuilder db, 
                                                  TestQuery query);
                                                
}
