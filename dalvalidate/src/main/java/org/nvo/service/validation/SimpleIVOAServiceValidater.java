package org.nvo.service.validation;

import net.ivoa.util.Configuration;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;
import java.util.Date;
import java.util.Properties;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.text.DateFormat;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.parsers.ParserConfigurationException; 
import javax.xml.transform.TransformerFactory;

import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

/**
 * a simple, synchronous implementation of a validater of a "simple"-style 
 * service.  Simple IVOA services (like ConeSearch, SIA, SSA, etc.) take a
 * URL (with arguments) as input and returns results in a VOTable.
 * This implementation sends sequence of queries to a service, identified by 
 * a base URL, and evaluates the results.  
 */
public class SimpleIVOAServiceValidater {

    protected Configuration config = null;
    protected Evaluator eval = null;
    protected LinkedList stdqueries = new LinkedList();
    protected TransformerFactory tfact = null;
    protected DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
    protected static StatusHelper statushelper = new StatusHelper();
    protected boolean verbose = false;
    protected String resultRootName = "IVOAServiceValidation";
    protected String queryRootName = null;
    protected ResultTypes defResultTypes = new ResultTypes(ResultTypes.ADVICE);

    private HashSet validating = new HashSet();

    /**
     * Construct the validater
     * @param config    the configuration for this validater.  It is 
     *                    assumed that 
     */
    public SimpleIVOAServiceValidater(Configuration config) { 
        this(config, null, null);
    }

    /**
     * create a Validater
     * @param config    the URL for the configuration file containing the 
     *                    queries to test
     */
    public SimpleIVOAServiceValidater(URL config) 
         throws ParserConfigurationException, SAXException, IOException
    {
        this(config, null);
    }

    /**
     * create a Validater
     * @param config    the URL for the configuration file containing the 
     *                    queries to test
     * @param lookup    the class to use to lookup the location of stylesheets
     *                    referenced in the configuration.  
     */
    public SimpleIVOAServiceValidater(URL config, Class lookup) 
         throws ParserConfigurationException, SAXException, IOException
    {
        this(new Configuration(config.toString(), lookup), null, lookup);
    }

    /**
     * create a Validater
     * @param config    the configuration containing the queries to test
     * @param tf        the TransformFactory to use to create the XSLT
     * @param resClass  the class to use to lookup the location of 
     *                     stylesheets as resources.
     */
    public SimpleIVOAServiceValidater(Configuration config, 
                                      TransformerFactory tf, Class resClass) 
    {
        this.config = config;
        if (config == null) 
            throw new NullPointerException("Config not provided");

        df.setNamespaceAware(true);

        // factory for processing stylesheets
        tfact = tf;
        if (tfact == null) tfact = TransformerFactory.newInstance();

        String elname = config.getParameter("resultRoot");
        if (elname != null) resultRootName = elname;
        elname = config.getParameter("queryRoot");
        if (elname != null) queryRootName = elname;

        // create the Evaluator
        Configuration econfig = config.getConfiguration("evaluator", "type", 
                                                        "xsl");
        if (econfig == null) econfig = Configuration.makeEmpty("evaluator");
        eval = new VOTableXSLEvaluator(econfig, tfact, resClass);
        if (queryRootName != null) 
            ((XSLEvaluator) eval).setResponseRootName(queryRootName);

        // collect any configured queries
        Configuration tqconfig = 
                HTTPGetTestQuery.getTestQueryConfig(config, "httpget");
        if (tqconfig != null) 
            HTTPGetTestQuery.addAllQueryData(tqconfig, stdqueries);
    }

    /**
     * return the default result root name.  This may be overridden by 
     * the stylesheet.
     */
    public String getResultRootElementName() { return resultRootName; } 

    /**
     * return true if all exceptions will be reported via standard error.
     * The default is false.
     */
    public boolean isVerbose() { return verbose; }

    /**
     * set whether all exceptions will be reported via standard error.
     * @param yes     true, if all exceptions should be reported
     */
    public void setVerbose(boolean yes) { verbose = yes; }

    /**
     * set the default result types to include in the evaluation.  Individual
     * validation test queries may overrid this.  
     * @param tokens   the types provided as a space-delimited set of tokens
     *                 (e.g., "fail warn rec pass", to see everything).
     */
    public void setDefaultResultTypes(String tokens) {
        defResultTypes.setTypes(tokens);
    }

    /**
     * validate a service accessed via a given base URL
     * @param baseURL       the base URL of the service to validate
     * @param userQueries   an array of queries provided by the caller to 
     *                        include of in the battery of queries that will
     *                        be sent to the service.  If null (or 
     *                        zero-length), only the standard queries provided
     *                        at configuration time will be sent.  
     * @param listener      a listener to alert about the progress of the 
     *                        validation.  If null, progress will not be 
     *                        reported.  
     * @exception DOMException  if a problem occurs while generating the output
     *                        document.  
     */
    public Document validate(String baseURL, 
                             HTTPGetTestQuery.QueryData[] userQueries,
                             ValidaterListener listener)
         throws DOMException, TestingException, ParserConfigurationException,
                MalformedURLException 
    {
        return validate(baseURL, userQueries, listener, null);
    }

    /**
     * validate a service accessed via a given base URL.  
     * 
     * This method will react to thread interruptions and to calls 
     * {@link #interrupt() this object's interrupt() method}.  It will 
     * attempt to bring its work to a good stopping point and return 
     * whatever results it has accumulated so far.  Before exiting, it 
     * will call Thread.interrupt() to give the caller a chance to 
     * halt its work, too.  
     *
     * @param baseURL       the base URL of the service to validate
     * @param userQueries   an array of queries provided by the caller to 
     *                        include of in the battery of queries that will
     *                        be sent to the service.  If null (or 
     *                        zero-length), only the standard queries provided
     *                        at configuration time will be sent.  
     * @param listener      a listener to alert about the progress of the 
     *                        validation.  If null, progress will not be 
     *                        reported.  
     * @exception DOMException  if a problem occurs while generating the output
     *                        document.  
     */
    public Document validate(String baseURL, 
                             HTTPGetTestQuery.QueryData[] userQueries,
                             ValidaterListener listener,
                             String resultsName)
         throws DOMException, TestingException, ParserConfigurationException,
                MalformedURLException 
    {
      int count = 0, ntests = 0;
      try {
        validating();

        LinkedList queries = (LinkedList) stdqueries.clone();
        if (userQueries != null) {
            for(int i=userQueries.length-1; i >= 0; i--) 
                queries.addFirst(userQueries[i]);
        }

        Map status = null;
        String id = null;
        if (listener != null) {
            status = statushelper.newStatus();
            id = (String) status.get("id");
            status.put("totalQueryCount", new Integer(queries.size()));
        }

        HTTPGetTestQuery tq = new HTTPGetTestQuery(baseURL, new Properties());
        tq.setResultTypes(defResultTypes.getTypes());
        ListIterator it = queries.listIterator();

        if (! it.hasNext()) 
            throw new ConfigurationException("no queries configured, " + 
                                             "none provided");

        Document resdoc = df.newDocumentBuilder().newDocument();
        Element results = resdoc.createElement(resultRootName);
        results.setAttribute("baseURL", baseURL);
        if (resultsName != null) 
            results.setAttribute("name", resultsName);
        resdoc.appendChild(results);
            
        while (it.hasNext()) {
            tq.setQueryData((HTTPGetTestQuery.QueryData) it.next());

            if (listener != null) {
                statushelper.setNextQuery(status, tq.getName(), 
                                          tq.getDescription());
                listener.progressUpdated(id, false, status);
            }

            // process query
            SimpleTester tester = new SimpleTester(tq, eval);
            try {
                status.remove("exception");

                if (Thread.interrupted()) throw new 
                    InterruptedException("Validation shutdown requested");

                ntests = tester.applyTests(results);
                count += ntests;
                status.put("ok", Boolean.TRUE);
                status.put("queryTestCount", new Integer(ntests));
                status.put("totalTestCount", new Integer(count));
            }
            catch (ConfigurationException ex) {
                String message = "Internal Validater Error: "+ ex.getMessage();

                String date = DateFormat.getDateInstance().format(new Date());
                System.err.println(date + ": baseURL=" + baseURL + "\n  " + 
                                   message);
                ex.printStackTrace();

                message += " (Please report to validater service provider.)";
                status.put("ok", Boolean.FALSE);
                status.put("message", message);
                status.put("exception", ex);
                addTestingFailure(results, tq, "validater", message);
            }
            catch (TimeoutException ex) {
                String message = "No response from service (" +
                    ex.getMessage() + ")";
                if (verbose) {
                    String date = 
                        DateFormat.getDateInstance().format(new Date());
                    System.err.println(date + ": baseURL=" + baseURL + "\n  " + 
                                       message);
                }

                status.put("ok", Boolean.FALSE);
                status.put("message", message);
                addTestingFailure(results, tq, "timeout", message);
                // break;
            }
            catch (TestingException ex) {
                String message = addCommFailure(results, tq, ex);
                if (verbose) {
                    String date = 
                        DateFormat.getDateInstance().format(new Date());
                    System.err.println(date + ": baseURL=" + baseURL + "\n  " + 
                                       message);
                }

                status.put("ok", Boolean.FALSE);
                status.put("message", message);
            }
            catch (InterruptedException ex) {
                String message = 
                    "Testing interrupted while waiting for response";
                if (verbose) {
                    String date = 
                        DateFormat.getDateInstance().format(new Date());
                    System.err.println(date + ": baseURL=" + baseURL + "\n  " + 
                                       message);
                }

                status.put("ok", Boolean.FALSE);
                status.put("message", message);

                addTestingFailure(results, tq, "interrupted", message);

                // this will signal the caller to clean up
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (listener != null) {
            statushelper.setNextQuery(status, null, null);
            status.put("done", Boolean.TRUE);
            listener.progressUpdated(id, true, status);
        }

        return resdoc;

      } finally {
        doneValidating();
      }
    }

    /**
     * add a test result to a given XML document reporting an exception-
     * generating problem.
     * @param root      the element to add the result node to
     * @param tq        the TestQuery that failed
     * @param testname  the item name to give this result
     * @param message   the description of the failure
     */
    protected void addTestingFailure(Element root, TestQuery tq, 
                                     String testname, String message)
         throws DOMException
    {
        Document doc = root.getOwnerDocument();

        String rootname = queryRootName;
        if (rootname == null) 
            rootname = getCurrentQueryRootName(root);
        if (rootname == null) rootname = "testQuery";

        Element battery = doc.createElement(rootname);
        battery.setAttribute("name", tq.getName());
        battery.setAttribute("role", tq.getType());
        battery.setAttribute("showStatus", tq.getResultTokens());

        if (tq instanceof HTTPGetTestQuery) 
            battery.setAttribute("options", 
                                 ((HTTPGetTestQuery) tq).getURLArgs());

        Element test = doc.createElement("test");
        test.setAttribute("item", testname);
        test.setAttribute("status", "fail");

        test.appendChild(doc.createTextNode(message));
        battery.appendChild(test);
        root.appendChild(battery);
    }

    private String getCurrentQueryRootName(Element parent) {
        Node child = parent.getLastChild();
        if (child == null) return null;

        while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
            child = child.getPreviousSibling();
        }

        String out = null;
        if (child != null) out = child.getNodeName();
        return out;
    }

    /**
     * add a communication error and return an appropriate user message
     */
    protected String addCommFailure(Element root, TestQuery tq, Exception ex) 
         throws DOMException
    {
        Class excl = ex.getClass();
        String exname = excl.getName().substring(
                                     excl.getPackage().getName().length()+1);

        String out = "Apparent communication error produced an exception " +
            "inside the validater: \n" + exname + ": " + ex.getMessage();

        String item = "comm";
        if (ex instanceof UnrecognizedResponseTypeException) 
            item = "responseType";

        addTestingFailure(root, tq, item, 
                          out + "\n(Is your network up? Behind a firewall?)");

        return out;
    }

    /**
     * signal all threads currently running validate() to interrupt their 
     * testing and exit as soon as possible.  
     */
    public final synchronized void interrupt() { 
        Iterator it = validating.iterator();
        while (it.hasNext()) {
            ((Thread) it.next()).interrupt();
        }
    }

    /**
     * return true if there is at least one Thread executing the 
     * validate method.
     */
    public boolean isValidating() {  return (validating.size() > 0); }

    /**
     * wait for all validating threads to complete their validation.  That is,
     * wait up to the given amount of time for the {@link #isValidating()} 
     * method to return false.
     * @param millis   the amount of time to wait.  If <= 0, wait indefinitely.
     */
    public void waitForValidation(long millis) throws InterruptedException {
        long start = (new Date()).getTime();
        long now = start;
        long step = 100;
        while (! isValidating() && (millis <= 0 || now-start > millis)) {
            Thread.sleep(step);
            now = (new Date()).getTime();
            if (step < 2000) step *= 2;
        }
    }

    /**
     * registers the current thread as validating.  This should be called 
     * at the start of the execution of validate() method so that it can
     * be cleanly interrupted via this class's {@link #interrupt() interrupt()}
     * method.  
     */
    protected final synchronized void validating() {
        validating.add(Thread.currentThread());
    }

    /**
     * unregister the current thread as validating.  This should be called 
     * at the end of the validate() method via a finally block.  
     */
    protected final synchronized void doneValidating() {
        validating.remove(Thread.currentThread());
    }

}
