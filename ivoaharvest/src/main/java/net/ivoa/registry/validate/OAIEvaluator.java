package net.ivoa.registry.validate;

import net.ivoa.util.Configuration;

import org.nvo.service.validation.EvaluatorBase;
import org.nvo.service.validation.TestQuery;
import org.nvo.service.validation.TestingException;
import org.nvo.service.validation.TestingIOException;
import org.nvo.service.validation.ResultTypes;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.IOException;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * an evalutor of an OAI-PMH compliant service response.  It employs the OAI 
 * Explorer validation tool, either as a local binary executable or as the 
 * web-accessible service.  It can invoke the tool, scrape out the results, 
 * and encode them into an XML format.
 */
public class OAIEvaluator extends EvaluatorBase {
    // the default name of the element containing a single test result
    // in the results of applying these tests.  
    protected final static String TEST_RESULT_NODE = "test";
    protected final static String TEST_QUERY_NODE = "testQuery";

    protected String testElemName = TEST_RESULT_NODE;
    protected String queryElemName = TEST_QUERY_NODE; 

    /**
     * create an OAI evaluator.
     */
    public OAIEvaluator() { }

    /**
     * apply the tests against the response from a TestQuery.  The query
     * will be invoke and managed internally via the given TestQuery, 
     * heeding its advice as applicable.  Normally, this method is passed 
     * an OAIExplorerTestQuery as its TestQuery.
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
        throws TestingException, InterruptedException
    {
        try {
            return applyTests(EvaluatorBase.getStream(tq), tq, addTo);
        }
        catch (IOException ex) {
            throw new TestingIOException(ex);
        }
    }

    /**
     * apply the tests against the response from a TestQuery.  The query
     * will be invoke and managed internally via the given TestQuery, 
     * heeding its advice as applicable.
     * @param response  the response stream that results from invoking 
     *                     the TestQuery, tq.
     * @param tq        the test to invoke
     * @param addTo     the element to append the XML-encoded results into.
     * @return int    the number of test results written into addTo.  Note
     *                  that the actual number executed may have been higher.
     * @exception ProcessingException  if a (SAX) XML processing exception is 
     *                  encountered.  Normally, exceptions specifically related 
     *                  to parsing (which might internally throw a 
     *                  SAXParsingException) should not occur if the 
     *                  setParsingErrorHandler() will return a non-null object.
     * @exception InterruptedException   if an interrupt signal was sent to 
     *                to this evaluator telling it to stop testing.
     * @exception TestingException   if any other non-recoverable error is 
     *                encountered while applying tests.
     */
    public int applyTests(InputStream response, TestQuery tq, Element addTo)
         throws TestingException, InterruptedException
    {
        try {
            BufferedReader results = 
                new BufferedReader(new InputStreamReader(response));

            String line = results.readLine();
            while (line.trim().length() > 0) { line = results.readLine(); }
            int p = -1;
            if (line.indexOf("<!") >= 0 || line.indexOf("<?xml ") >= 0 || 
                line.indexOf("<html") >= 0) 
                {
                    // if we are going through a remote web service, skip 
                    // down to the results section
                    while (line != null && 
                           (p = line.indexOf("<pre>")) < 0) 
                        { 
                            line = results.readLine(); 
                        }
            
                    if (p < 0) 
                        throw new OAIExplorerException("No formatted results " + 
                                                       "returned from service");
                    line = line.substring(p+5);
                }

            String testmatch = ") Testing :";
            String urlmatch = "URL :";
            String resultmatch = "Test Result :";
            String emsgmatch = "**** [";
            String query = null;
            String queryName = null, num = null;
            Element tnode = null, qnode = null;
            int rt = tq.getResultTypes();
            boolean showPass = (rt & ResultTypes.PASS) > 0;
            boolean showFail = (rt & ResultTypes.FAIL) > 0;
            boolean showWarn = (rt & ResultTypes.WARN) > 0;
            boolean show = true;

            while (line != null) {

                // find start of next test 
                while (line != null && ((p = line.indexOf(testmatch)) < 0)) { 
                    line = results.readLine();
                }
                if (line == null) continue;

                // pull out the name and number of the test query
                queryName = line.substring(p + testmatch.length()).trim();
                num = line.substring(0,p);
                p = num.indexOf("(");
                num = num.substring(p+1);

                line = results.readLine();
                if (line == null) {
                    throw new OAIExplorerException("Premature end while " +
                                                   "looking for test URL for "+
                                                   queryName);
                }

                // find the url for the test query
                query = "";
                if ((p = line.indexOf(urlmatch)) >= 0) {
                    query = line.substring(p+urlmatch.length()).trim();
                    p = query.indexOf("?");
                    if (p >= 0) query = query.substring(p+1);
                }

                qnode = addQueryNode(addTo, queryName, query);

                // now grab the results
                show = true;
                line = results.readLine();
                while (line != null && ((p = line.indexOf(resultmatch)) < 0)) {
                    line = results.readLine();
                }
                if (line == null) 
                    throw new OAIExplorerException("Premature end while " +
                                                   "looking for test results "+
                                                   "for " + queryName);
                String status = line.substring(p + resultmatch.length()).trim();
                if (status.startsWith("FAIL")) {
                    status = "fail";
                    if (! showFail) show = false;
                }
                else if (status.startsWith("OK")) {
                    status = "pass";
                    if (! showPass) show = false;
                }
                else {
                    status = "warn";
                    if (! showWarn) show = false;
                }

                if (show) addTestNode(qnode, "summary", status, null);

                // now catch any error or warning messages
                show = true;
                line = results.readLine();
                int t = 0;
                while (line != null && ((p = line.indexOf(testmatch)) < 0)) {
                    p = line.indexOf(emsgmatch);
                    if (p >= 0) {
                        String msg = null;
                        line = line.substring(p + emsgmatch.length());
                        p = line.indexOf("]");
                        status = (p < 0) ? line.trim() 
                                         : line.substring(0,p).trim();
                        if (status.equals("ERROR")) {
                            status = "fail";
                            msg = line.substring(p+1).trim();
                            if (! showFail) show = false;
                        }
                        else if (status.equals("WARNING")) {
                            status = "warn";
                            msg = line.substring(p+1).trim();
                            if (! showWarn) show = false;
                        }
                        else {
                            msg = status;
                            status = "warn";
                            if (! showWarn) show = false;
                        }

                        if (show) 
                            addTestNode(qnode, "oai-" + num + "-" + (++t), 
                                        status, msg);
                    }

                    line = results.readLine();
                }
            }
        }
        catch (IOException ex) {
            throw new TestingIOException(ex);
        }
        
        return countResults(addTo);
    }    

    private int countResults(Element testQuery) {
        NodeList nl = testQuery.getElementsByTagName(testElemName);
        return ((nl == null) ? 0 : nl.getLength());
    }

    /**
     * add a query node to the given element
     * @return Element   the added node
     */
    Element addQueryNode(Element parent, String name, String queryArgs) {
        Document doc = parent.getOwnerDocument();
        Element qnode = doc.createElement(getQueryElemName());
        qnode.setAttribute("role", "oai");
        qnode.setAttribute("name", name);
        qnode.setAttribute("options", queryArgs);
        qnode.appendChild(doc.createTextNode("\n  "));
        parent.appendChild(doc.createTextNode("  "));
        parent.appendChild(qnode);
        parent.appendChild(doc.createTextNode("\n"));
        return qnode;
    }

    /**
     * add a test node to the given element
     * @return Element   the added node
     */
    Element addTestNode(Element parent, String name, String status, 
                        String msg) 
    {
        if (msg == null) 
            msg = "Operation must be compliant with OAI-PMH standard";
        Document doc = parent.getOwnerDocument();
        Element tnode = doc.createElement(testElemName);
        tnode.setAttribute("item", name);
        tnode.setAttribute("status", status);
        tnode.appendChild(doc.createTextNode(msg));
        parent.appendChild(doc.createTextNode("  "));
        parent.appendChild(tnode);
        parent.appendChild(doc.createTextNode("\n  "));
        return tnode;
    }

    /**
     * set the name to be used as the root element of the response
     * @param name   the element name to use
     */
    public void setQueryElemName(String name) {
        queryElemName = name;
    }

    /**
     * return the name of the root element of the XML response.
     */
    public String getQueryElemName() {  return queryElemName;  }

}
