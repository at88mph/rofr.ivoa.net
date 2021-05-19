package org.nvo.service.validation;

import net.ivoa.util.Configuration;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.parsers.ParserConfigurationException; 

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;


/**
 * A class that uses XSL to apply tests to the response from a test query.  
 *
 * Typically, this class is subclassed to handle a specific kind of service.
 * In particular,
 * <ul>
 *    <li> the {@link #determineResponseType(Document) determineResponseType()}
 *         method is overridden to recognize the possible XML responses from 
 *         the service </li>
 *    <li> the {@link #setTransformationParams(Transformer, TestQuery) setTransformationParams()} 
 *         is overridden to set any special stylesheet parameters.  </li>
 * </ul>
 * Nevertheless, this class can be used directly, though with reduced 
 * functionality.  The 
 * {@link #setDefaultResponseType(String) setDefaultResponseType()} method 
 * must be called explicitly, though, to get it to work (see also 
 * <strong>Configuration</strong> below).  
 *
 * <strong>Configuration</strong>
 *
 * This class can be configured via a Configuration object.  When passed 
 * to the constructor, it will be searched for the following elements:
 * <dl>
 * <dt><pre>&lt;stylesheet responseType="<i>identifier</i>"><i>stylesheet_name</i>&lt;/stylesheet></pre> </dt>
 * <dd> use the stylesheet called <i>stylesheet_name</i> when the 
 *      {@link #determineResponseType(Document)} method returns the 
 *      response type string given by <i>identifier</i>. </dd>
 * <dt><pre>&lt;stylesheetDir><i>directory</i>&lt;/stylesheetDir></pre> </dt>
 * <dd> look for stylesheets in the named directory.  If not absolute, a 
 *      directory relative to the current working directory will be searched.
 *      If not provided, it will be assumed that the stylesheets should be
 *      loaded as resources.  </dd>
 * <dt> <pre>&lt;defaultResponseType><i>key</i>&lt;/defaultResponseType></pre> </dt>
 * <dd> the response type identifier to assume if the response is not 
 *      recognized.  This is usually only set if this XSLEvaluator class is 
 *      is being used directly rather than through a subclass.  </dd>
 * </dl>
 */
public class XSLEvaluator extends EvaluatorBase 
    implements XMLResponseEvaluator 
{
    // the default name of the element containing a single test result
    // in the results of applying these tests.  
    protected final static String TEST_RESULT_NODE = "test";

    protected DocumentBuilderFactory df = null;
    protected TransformerFactory tfact = null;
    protected Hashtable transformers = new Hashtable();
    protected Hashtable stylesheets = new Hashtable();
    protected Class wrtClass = this.getClass();
    protected String test_result_node = TEST_RESULT_NODE;
    protected String defaultResponseType = null;
    protected String respRootName = null; 
    protected ErrorHandler errhandler = null;

    /**
     * Create an Evaluator.  The caller will need to provide a stylesheet
     * by calling {@link #useStylesheet(String,String) useStylesheet()}.  
     */
    public XSLEvaluator() {
        this(null, null);
    }

    /**
     * Create an Evaluator 
     * @param transfact   a TransformerFactory object used to create
     *                     new XSL Transoformers
     * @param resClass    the class to use to lookup the location of 
     *                     stylesheets as resources.
     */
    public XSLEvaluator(TransformerFactory transfact, Class resClass) {
        this(transfact, resClass, null);
    }

    /**
     * Create an Evaluator 
     * @param transfact   a TransformerFactory object used to create
     *                     new XSL Transoformers
     * @param resClass    the class to use to lookup the location of 
     *                     stylesheets as resources.
     */
    public XSLEvaluator(TransformerFactory transfact, Class resClass, 
                        Properties props) 
    {
        super(props);
        tfact = transfact;
        if (tfact == null) tfact = TransformerFactory.newInstance();
        if (resClass != null) wrtClass = resClass;

        df = DocumentBuilderFactory.newInstance();
        df.setNamespaceAware(true);
    }

    /**
     * Create an Evaluator. 
     * @param config      an Evaluator configuration block.  
     * @param transfact   a TransformerFactory object used to create
     *                     new XSL Transoformers
     * @param resClass    the class to use to lookup the location of 
     *                     stylesheets as resources.
     */
    public XSLEvaluator(Configuration config, TransformerFactory transfact, 
                        Class resClass) 
    {
        this(transfact, resClass);

        File stylesheetDir = null;
        String ssdir = config.getParameter("stylesheetDir");
        if (ssdir != null && ssdir.length() == 0) ssdir = null;
        if (ssdir != null) 
            stylesheetDir = new File(ssdir);

        int i;
        String responseType = null, sheet = null, empty="";
        Configuration[] sheets = config.getBlocks("stylesheet");
        for(i=0; i < sheets.length; i++) {
            responseType = sheets[i].getParameter("@responseType");
            if (responseType == null) responseType = empty;
            sheet = sheets[i].getParameter(empty);
            if (sheet != null && sheet.length() > 0) {
                if (stylesheetDir != null) 
                    sheet = (new File(stylesheetDir, sheet)).getAbsolutePath();
                useStylesheet(responseType, sheet);
            }
        }

        String cname = null;
        Configuration[] cprops = config.getBlocks("property");
        for(i=0; i < cprops.length; i++) {
            cname = cprops[i].getParameter("@name");
            if (cname != null && cname.length() > 0) 
                setProperty(cname, cprops[i].getParameter(empty));
        }

        defaultResponseType = config.getParameter("defaultResponseType");
    }

    /**
     * Create an Evaluator. 
     * @param config      an Evaluator configuration block.  
     */
    public XSLEvaluator(Configuration config) {
        this(config, null, null);
    }

    /**
     * return the DocumentBuilderFactory that will be used to create the XML
     * parser that will parse the query response.  This is useful to call
     * when it is necessary to configure the factory to enable XML validation
     */
    public DocumentBuilderFactory getDocumentBuilderFactory() { return df; }

    /**
     * set the name to be used as the root element of the response
     * @param name   the element name to use
     */
    public void setResponseRootName(String name) {
        respRootName = name;
    }

    /**
     * return the name of the root element of the XML response.
     */
    public String getResponseRootName() {  return respRootName;  }

    /**
     * get the responseType key string that will be assumed if 
     * if this instance does not recognize the response format or type.
     * By default this value is null, which means that an unrecognized
     * response will cause an UnrecognizedResponseTypeException will 
     * thrown.  Subclass implementations may choose to ignore this 
     * value.  
     * @see #setDefaultResponseType(String)
     */
    public String getDefaultResponseType() { return defaultResponseType; } 

    /**
     * set the responseType key string that should be assumed if 
     * if this instance does not recognize the response format or type.
     * By default this value is null, which means that an unrecognized
     * response will cause an UnrecognizedResponseTypeException will 
     * thrown.  If XSLEvaluator is to be used directly, rather than via
     * a subclass, then this must be set to a non-null value.  Note that
     * a value can also be set via the Configuration provided at construction
     * time.  Subclass implementations may choose to ignore this 
     * value.  
     * @see #determineResponseType(Document)
     */
    public void setDefaultResponseType(String type) { 
        defaultResponseType = type; 
    }

    /**
     * apply the tests against the response from a TestQuery.  The query
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
         throws TestingException, InterruptedException
    {
        try {
            String responseType = determineResponseType(response);
            Transformer transf = getTransformerFor(responseType);

            // reset the transformation parameters
            setTransformationParams(transf, tq);

            // pipe the xml data stream through the stylesheet and into 
            // the output DOM
            DOMResult dr = new DOMResult(addTo);
            transf.transform(new DOMSource(response), dr);

            return countResults(addTo);
        }
        catch (TransformerConfigurationException ex) {
            throw new ConfigurationException(ex);
        }
        catch (IOException ex) {
            throw new ConfigurationException(ex);
        }
        catch (TransformerException ex) {
            throw new TestingException("XSL tranformation failure: " + 
                                       ex.getMessage());
        }
    }

    int countResults(Element battery) {
        NodeList tests = battery.getElementsByTagName(test_result_node);
        return tests.getLength();
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
        Document respdoc = null;
        int count = 0;

        DocumentBuilder db = null;
        ParsingErrors pe = null;
        try {
            db = df.newDocumentBuilder();
            pe = applyParsingErrorHandler(db, tq);

            respdoc = db.parse(new InputSource(response));
        }
        catch (ParserConfigurationException ex) {
            throw new ConfigurationException(ex);
        }
        catch (SAXParseException ex) {
            // System.err.println(ex.getMessage());
            // this should have been caught by the ParsingErrors object;
            // only deal with it here if we don't have a ParsingErrors object
            if (pe == null) {
                throw new ProcessingException(ex);
            }
        }
        catch (SAXException ex) {
            throw new ProcessingException(ex);
        }
        catch (IOException ex) {
            throw new TestingIOException(ex);
        }

        if (respdoc != null) 
            count = applyTests(respdoc, tq, addTo);

        if (pe != null) {
            Document outdoc = addTo.getOwnerDocument();
            Element thisresult = getLastChildElement(addTo);
            if (thisresult == null) 
                throw new ConfigurationException("Missing evaluation result!");
            Node insertPoint = getLastChildElement(thisresult);
            if (insertPoint != null) insertPoint = insertPoint.getNextSibling();
            pe.insertErrors(thisresult, insertPoint, 
                            outdoc.createTextNode("\n    "));
        }

        return count;
    }

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
     * set the ErrorHandler that should be used by applyTests() 
     * when parsing a query response.  If this is an instance of a 
     * {@link ParsingErrors}, it will be returned by 
     * {@link #applyParsingErrorHandler(DocumentBuilder,TestQuery) applyParsingErrorHandler}.  
     * @param eh   the ErrorHandler to use
     */
    public void setParsingErrorHandler(ErrorHandler eh) { errhandler = eh; }

    /**
     * get the ErrorHandler that will be used by applyTests() 
     * when parsing a query response.  Note that this will often be a 
     * {@link ParsingErrors} instance.  
     */
    public ErrorHandler getParsingErrorHandler() { return errhandler; }

    /**
     * if desired, set the ErrorHandler for the parser.  This implementation
     * applys the error handler set via 
     * {@link setParsingErrorHandler(ErrorHandler) setParsingErrorHandler()}.
     * If the error handler is a {@link ParsingErrors}, any previously 
     * captured parsing errors will be cleared out.
     * @param db       the DOM parser
     * @param query    the TestQuery being handled
     * @return ParsingErrors  the ErrorHandler object that was set.
     */
    public ParsingErrors applyParsingErrorHandler(DocumentBuilder db,
                                                  TestQuery query)
    {
        ParsingErrors pe = null;
        if (errhandler != null) {
            db.setErrorHandler(errhandler);
            if (errhandler instanceof ParsingErrors) {
                pe = (ParsingErrors) errhandler;
                pe.clear();
            }
        }

        return pe;
    }

    /**
     * get the transformer object that can handle the given format
     * @param key   an identifier for the response type, usually returned
     *                by determineResponseType()
     */
    public Transformer getTransformerFor(String key) 
         throws TransformerConfigurationException, IOException
    {
        Templates tmpls = (Templates) transformers.get(key);
        if (tmpls == null) {
            tmpls = createTemplatesFor(key);
            if (tmpls != null) transformers.put(key, tmpls);
        }
        return ((tmpls != null) ? tmpls.newTransformer() : null);
    }

    /**
     * examine the response from the service to determine which stylesheet 
     * to apply to it. <p>
     *
     * Specific service validators should override this method.  This 
     * default implementation does not look at the response but simply
     * returns an empty string.  
     * @param serviceResponse   the XML response from the service
     * @return String   the label that should be used to retrieve the 
     *                     stylesheet file name from the configuration.
     * @exception UnrecognizedResultTypeException  if the contents are not
     *     recognized.  This simple implementation never throws this.  
     */
    public String determineResponseType(Document serviceResponse) 
         throws UnrecognizedResponseTypeException
    {
        if (defaultResponseType == null)
            throw new UnrecognizedResponseTypeException("unrecognized " + 
                                                        "response from " + 
                                                        "service");
        return defaultResponseType;
    }

    /**
     * create a stylesheet transformation for a given result type.  
     */
    protected Templates createTemplatesFor(String responseType) 
         throws TransformerConfigurationException, IOException
    {
        String sheet = (String) stylesheets.get(responseType);
        if (sheet == null) 
           throw new TransformerConfigurationException(
           "No stylesheet configured for response type=\""+ responseType+"\"");

        // parse the stylesheet
        String ssurl = null;
        try {
            ssurl = Configuration.findURL(sheet, wrtClass);
        }
        catch (FileNotFoundException ex) {
             throw new IllegalStateException(ex.getMessage());
        }
        return tfact.newTemplates(new StreamSource(ssurl));
    }

    /**
     * set any parameters for the transforming stylesheet.  This implementation
     * sets a standard set; subclasses should be sure to first call this one
     * before adding additional parameters.  Currently, the following parameters
     * are set:
     * <pre>
     *    Parameter Name       Value from
     *    ------------------   ------------------------
     *    query                query.getType()
     *    queryName            query.getName()
     *    inputs               query.getURLArgs()
     *    showStatus           query.getResultTokens()
     *    ignoreTests          query.getIgnorableTests()
     *    baseurl              baseURL
     *    resultsRootElement   this.getResponseRootName() (if not null)
     * </pre>
     * In addition, all properties found in props will be passed as parameters.
     */
    public void setTransformationParams(Transformer transf, TestQuery query) {
        Enumeration e;
        String name = null;
        String val = null;

        transf.clearParameters();

        // set the default evaluation properties
        for(e=evalprops.keys(); e.hasMoreElements();) {
            name = (String) e.nextElement();
            val = evalprops.getProperty(name);
            if (val != null) transf.setParameter(name, val);
        }

        setTransParam(transf, "queryType", query.getType());
        setTransParam(transf, "queryName", query.getName());
        setTransParam(transf, "queryDesc", query.getDescription());
        setTransParam(transf, "showStatus", query.getResultTokens());
        setTransParam(transf, "ignoreTests", query.getIgnorableTests());

        if (query instanceof HTTPGetTestQuery) {
            HTTPGetTestQuery tq = (HTTPGetTestQuery) query;
            transf.setParameter("inputs", tq.getURLArgs().trim());
            transf.setParameter("baseurl", tq.getBaseURL());
        }

        if (respRootName != null) 
            transf.setParameter("resultsRootElement", respRootName);

        Properties props = query.getEvaluationProperties();
        if (props != null) {
            for(e=props.keys(); e.hasMoreElements();) {
                name = (String) e.nextElement();
                val = props.getProperty(name);
                if (val != null) transf.setParameter(name, val);
            }
        }
    }

    private void setTransParam(Transformer trans, String param, Object value) {
        if (value != null) trans.setParameter(param, value);
    }

    /**
     * register a stylesheet with a response type.  If only one stylesheet
     * is needed then it can be added using a zero-length responseType
     * string, and this class can be used as is, without subclassing.  
     */
    public void useStylesheet(String responseType, String stylesheetfile) {
        stylesheets.put(responseType, stylesheetfile);
        transformers.remove(responseType);
    }

}
