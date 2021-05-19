package org.nvo.service.validation.webapp;

import net.ivoa.util.Configuration;
import org.nvo.service.validation.SimpleIVOAServiceValidater;
import org.nvo.service.validation.HTTPGetTestQuery;
import org.nvo.service.validation.ResultTypes;
import org.nvo.service.validation.TestingException;
import org.nvo.service.validation.ValidaterListener;

// import org.nvo.service.validation.webapp.ValidationException;
// import org.nvo.service.validation.webapp.ValidationSession;
// import org.nvo.service.validation.webapp.ValidationSessionBase;
// import org.nvo.service.validation.webapp.InternalServerException;
// import org.nvo.service.validation.webapp.BadRequestException;
// import org.nvo.service.validation.webapp.UnavailableSessionException;

import java.util.Properties;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.text.DateFormat;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.reflect.Method;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;


/**
 * a validation session handler for simple IVOA services.  "Simple" services
 * are those that follow the IVOA Data Access Layer pattern where a service 
 * is invoked via an HTTP Get URL, inputs are provided as arguments to the 
 * URL, and the response is XML file.
 */
public class SimpleIVOAServiceValidationSession extends ValidationSessionBase 
    implements ValidaterListener
{
    static Hashtable operations = new Hashtable();
    static Hashtable templates = new Hashtable();

    static {
        loadOpMethods(SimpleIVOAServiceValidationSession.class, operations);
    }

    TransformerFactory tf = null;
    Configuration config = null;
    SimpleIVOAServiceValidater validater = null;
    String baseURL = null;
    String userquery = null;
    String format = null;
    String show = null;
    String statusmsg = "{ 'status': 'unavailable', 'message': " +
        "'initializing...'}";
    Properties ctypes = new Properties();
    Document results = null;
    Vector progress = new Vector();
    
    /**
     * create a session
     * @exception FileNotFoundException   if config is null and the default
     *                configuration file cannot be found
     */
    public SimpleIVOAServiceValidationSession() 
         throws IOException, SAXException
    {
        this(null, null, null);
    }

    /**
     * create a session
     * @exception FileNotFoundException   if config is null and the default
     *                configuration file cannot be found
     * @exception SAXException   if there is an XML syntax error in the default
     *                configuration file 
     */
    public SimpleIVOAServiceValidationSession(TransformerFactory tf, 
                                              Class resClass) 
         throws IOException, SAXException
    {
        this(null, tf, resClass);
    }

    /**
     * create a session
     * @exception FileNotFoundException   if config is null and the default
     *                configuration file cannot be found
     * @exception SAXException   if there is an XML syntax error in the default
     *                configuration file 
     */
    public SimpleIVOAServiceValidationSession(Configuration config, 
                                              TransformerFactory tf, 
                                              Class resClass) 
         throws IOException, SAXException
    {
        super();

        if (resClass == null) resClass = getClass();
        if (config == null) config = new Configuration("config.xml", resClass);
        if (tf == null) tf = TransformerFactory.newInstance();
        this.config = config;
        this.tf = tf;
        validater = new SimpleIVOAServiceValidater(config, tf, resClass);

        ctypes.setProperty("text", "text/plain");
        ctypes.setProperty("html", "text/html");
        ctypes.setProperty("xml", "application/xml");
        ctypes.setProperty("json", JSON_CONTENT_TYPE);
    }

    /**
     * lookup the method to call for the given operation name.
     */
    protected Method getOpMethod(String op) {
        return (Method) operations.get(op);
    }

    /**
     * initialize the session with an endpoint and parameters.  
     *
     * @param endpoint    the endpoint of the service to be validated
     * @param params      input parameters to the validation session.  This
     *                       can be null when none are provided.
     * @exception UnavailableSessionException  if the session is in an 
     *                       unavailable state and cannot be reset and/or 
     *                       initialized, or the inputs are incorrect or 
     *                       insufficient.
     * @return String     a unique identifier for the validation request being
     *                       handled by this ValidationSession
     */
    public String initialize(String endpoint, Properties params) 
         throws UnavailableSessionException 
    {
        if (available) end(true);

        statusmsg = "{ 'status': 'initializing', 'message': 'initializing...' }";

        // set the runid so that we can setup some cache space
        runid = newRequestID();

        baseURL = correctBaseURL(endpoint);
        userquery = (String) params.get("query");
        format = (String) params.get("format");
        show = (String) params.get("show");

        if (userquery == null || userquery.length() == 0) {
            userquery = makeUserQuery(params);
        }
        validater.setDefaultResultTypes(show);

        statusmsg = 
            "{ 'status': 'ready', 'message': 'Validater initialized.' }";
        return super.initialize(endpoint, params);
    }

    /**
     * make an argument string for the service to be validated from the user's
     * inputs.  Return null if a legal string could not be returned.
     * @param params   the Properties object containing the users inputs
     */
    protected String makeUserQuery(Properties params) {
        return null;
    }

    /**
     * check the current validation inputs and report any problems found 
     * into the given Properties object.  The key, when appropriate, will 
     * correspond to the name of the input having an error.
     * @param errorsFound   the Properties object to write error messages 
     *                          into.  If null, errors will not be reported
     *                          but the inputs will still be checked.
     * @return boolean   true if the inputs appear correct and false, if 
     *                      problems were found. 
     */
    public boolean checkInputs(Properties errorsFound) {
        boolean ok = true;
        if (baseURL == null || baseURL.length() == 0) {
            ok = false;
            if (errorsFound != null) 
                errorsFound.put("baseURL", "No baseURL provided");
        }
        else {
            try {
                new URL(baseURL.substring(0,baseURL.length()-1));
            }
            catch (MalformedURLException ex) {
                ok = false;
                if (errorsFound != null) 
                    errorsFound.put("baseURL", 
                                    "Bad base URL: " + ex.getMessage());
            }
        }

        if (userquery == null || userquery.length() == 0) {
            ok = false;
            if (errorsFound != null) 
                errorsFound.put("userquery", "No query provide");
        }
        else if (ok) {
            try {
                new URL(baseURL + userquery);
            }
            catch (MalformedURLException ex) {
                ok = false;
                if (errorsFound != null) 
                    errorsFound.put("userquery", 
                                    "Bad user query (" + userquery + "): " +
                                    ex.getMessage());
            }
        }

        if (format == null || format.length() == 0) {
            ok = false;
            if (errorsFound != null) 
                errorsFound.put("format", "No format requested");
        }
        else {
            if (! ctypes.containsKey(format)) {
                ok = false;
                if (errorsFound != null) 
                    errorsFound.put("format", 
                                    "No content type registered for the " +
                                    format + " format.");
            }
        }

        String results = " " + show + " ";
        if (results.indexOf(" pass ") < 0 &&
            results.indexOf(" fail ") < 0 &&
            results.indexOf(" warn ") < 0 &&
            results.indexOf(" rec " ) < 0   ) 
        {
            ok = false;
            if (errorsFound != null) 
                errorsFound.put("show", "No recognized result types requested");
        }

        return ok;
    }

    /**
     * report one or more errors to the client.  This is done by formatting
     * the given message into a response document and written into the 
     * HttpServletResponse object.  Note that this method must not require
     * that this session object be initialized in order to complete 
     * successfully.
     *
     * @param errors   a set of name error messages.  If the error is 
     *                    associated with a bad input parameter, the key 
     *                    should be the name of the parameter; otherwise,
     *                    the name is implementation specific.
     * @param format   the name of the format to be used in encoding the 
     *                    error message.  The allowed names include
     *                    "html", "xml", "text", and "json".
     * @param out      the response to write the error document out to.
     */
    public void reportErrors(Properties errors, String format, 
                             HttpServletResponse out)
         throws InternalServerException, IOException
    {
        // set the status message
        StringBuffer sb = 
            new StringBuffer("{ 'status': 'unavailable', 'message': ");
        sb.append("'unable to initialize due to problem with inputs', \n");
        sb.append("'problems': {\n");
        Enumeration e = errors.propertyNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            sb.append("  '");
            sb.append(key);
            sb.append("': '");
            sb.append(errors.getProperty(key));
            sb.append("'\n");
        }
        sb.append("}}\n");

        statusmsg = sb.toString();

//         try {
//             throw new IllegalStateException("getting stack");
//         }
//         catch (Exception ex) {
//             ex.printStackTrace();
//         }
        
        // now report to user
        PrintWriter client = null;
        if (format == null) format = "xml";

        if (format.equals("html")) {
            out.setContentType(getContentTypeForFormat(format));
            client = out.getWriter();
            printErrorsHTML(errors, client);
        }
        else if (format.equals("json")) {
            out.setContentType(getContentTypeForFormat(format));
            client = out.getWriter();
            printErrorsJSON(errors, client);
        }
        else if (format.equals("text")) {
            out.setContentType(getContentTypeForFormat(format));
            client = out.getWriter();
            printErrorsText(errors, client);
        }
        else {
            if (! format.equals("xml")) 
                errors.setProperty("errorFormat", "unsupported error format: " +
                                   format);
            out.setContentType(getContentTypeForFormat("xml"));
            client = out.getWriter();
            printErrorsXML(errors, client);
        }
        client.close();
    }

    private void printErrorsText(Properties errors, PrintWriter out) { 
        int sz = errors.size();
        out.print("Errors found in the following input parameter");
        if (sz > 0) out.print("s");
        out.println(":");

        Enumeration e = errors.propertyNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            out.print("  ");
            out.print(key);
            out.print(":\t");
            out.println(errors.getProperty(key));
        }
    }
    
    private void printErrorsHTML(Properties errors, PrintWriter out) { 
        out.println("<html><title>Validation Errors</title><body>");
        out.println("<div id=\"content\">");
        out.println("<h1>Errors found in input arguments:</h1><dl>");

        Enumeration e = errors.propertyNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            out.print("  <dt> ");
            out.print(key);
            out.println(" </dt>");
            out.print("  <dd> ");
            out.print(errors.getProperty(key));
            out.println(" </dd>\n");
        }
        out.println("</dl></div>");
        out.println("</body></html>");
    }
    
    private void printErrorsXML(Properties errors, PrintWriter out) { 
        String root = validater.getResultRootElementName();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" " + 
                    "standalone=\"yes\" ?>");
        out.print("<");
        out.print(root);
        out.println(">");
        out.println("  <Error on=\"input\">");
        Enumeration e = errors.propertyNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            out.print("    <message about=\"");
            out.print(key);
            out.print("\">");
            out.print(errors.getProperty(key));
            out.println("</message>");
        }
        out.print("  </Error>\n</");
        out.print(root);
        out.println(">");
    }
    
    private void printErrorsJSON(Properties errors, PrintWriter out) { 

        // assume the status message has errors in it already
        out.println(statusmsg);

//         out.println("{ 'status': 'error', 'problems': {");
//         Enumeration e = errors.propertyNames();
//         while (e.hasMoreElements()) {
//             String key = (String) e.nextElement();
//             out.print("  '");
//             out.print(key);
//             out.print("': '");
//             out.print(errors.getProperty(key));
//             out.println("'");
//         }
//         out.println("}}");
    }
    

    /**
     * return true is the input baseURL and query inputs are legal.
     */
    public boolean isOK() { 
        return (available && checkInputs(null));
    }

    public void progressUpdated(String id, boolean isdone, Map status) {
        StringBuffer sb = new StringBuffer("{ ");

        Object val = status.get("ok");
        boolean ok = (val == null) ? true : ((Boolean) val).booleanValue();

        sb.append("'status': ");
        if (! available) {
            sb.append("'unavailable'");
        }
        else if (isdone) {
            sb.append("'done'");
        }
        else {
            sb.append("'running'");
        }

        String[] names = { "message", "nextQueryName", "nextQueryDescription", 
                           "lastQueryName", "lastQueryDescription", 
                           "totalTestCount", "totalQueryCount" };
        for(int i=0; i < names.length; i++) {
            val = status.get(names[i]);
            if (val != null) {
                sb.append(", '").append(names[i]).append("': '").append(val);
                sb.append("'");
            }
        }

        sb.append(", progress: [");

        val = status.get(names[1]);
        if (val != null && ! val.equals("")) {
            StringBuffer p = new StringBuffer("Sending the ");
            p.append(val).append(" query");
            val = status.get(names[2]);
            if (val != null && ! val.equals("")) 
                p.append(" (").append(val).append(")");
            p.append("...");
            progress.addElement(p.toString());
        }

        for(Enumeration e=progress.elements(); e.hasMoreElements();) {
            sb.append("'").append(e.nextElement()).append("'");
            if (e.hasMoreElements()) sb.append(",");
        }
        if (isdone) {
            if (progress.size() > 0) sb.append(",");
            sb.append("'...done!'");
        }
        sb.append("]");

        sb.append(" }");

        statusmsg = sb.toString();
    }

    /**
     * invoke the default operation.  
     * @param params  input parameters to the operation.  This can be null 
     *                   if none are required by the operation.
     * @param out     the servlet response object to write out to
     * @exception UnavailableSessionException  if the session is in an 
     *                   unavailable state, possible corrupted.  
     * @exception InternalServerException  if the validating server encounters
     *                   an internal error (by no fault of the user).  An 
     *                   HTTP servlet would typically respond to this exception
     *                   with a 500 message.
     * @exception IOException  if an I/O problem occurs while writing the 
     *                   output.
     * @return boolean   true if the session's work can be considered complete,
     *                   allowing this object to be discarded.  
     */
    public boolean invokeDefaultOp(Properties params, HttpServletResponse out)
         throws ValidationException, IOException
    {
        doValidate(params, out);
        return done;
    }

    /**
     * if validation is underway, wait until it is completed.
     */
    public void waitForValidation() {
        try {
            validater.waitForValidation(0);
        }
        catch (InterruptedException ex) {  }
    }

    public void doValidate(Properties props, HttpServletResponse out)
         throws ValidationException, IOException
    {
        // if false (or not specified), we will return the results immediately;
        // otherwise, we will cache for a later call to this method.  Note 
        // that once the result is returned, it will be eligble for being 
        // cleaned up.  
        boolean cache = 
            Boolean.valueOf(props.getProperty("cache", "false")).booleanValue();

        Transformer printer = null;
        if (! cache) {
            // cache the printer
            try {
                printer = getTransformerForFormat(format);
            } catch (Exception ex) {
                throw new InternalServerException(ex);
            }
            if (printer == null) 
                // we've already checked the legitamacy of the format name, 
                // so assume it's a configuration problem.  
                throw new InternalServerException("missing format support: "
                                                  + format);
        }

        try {
            if (results == null) {

                // check to see if we are already validating; if so, wait.
                synchronized (this) {
                    if (results == null) {
                        if (validater.isValidating()) {
                            // should not happen
                            log("possible programmer error: " +
                                "waiting for validation threads");
                            waitForValidation();
                        }
                    }
                    if (results == null) {
                        results = validate(props, out);

                        // check for an interrupted thread.
                        if (Thread.interrupted()) {
                            log("Full validation interrupted before completion");
                            // we'll transform and send what we have
                        }
                    }
                }
            }

            // send requested response
            if (cache) {
                String baseResultURL = props.getProperty("validaterBaseURL");

                // we want the URL seen by the user to have all of the inputs 
                // from the form.  So include the queryString, minus the cache 
                // parameter.
                String queryString = props.getProperty("queryString");
                if (queryString != null) {
                    Pattern cshparm = Pattern.compile("&cache=[^&]*");
                    queryString = cshparm.matcher(queryString).replaceAll("");
                }

                // just cache the results for later retrieval:
                // tell the user where to get the results
                StringBuffer json = 
                    new StringBuffer("{ status: 'done', resultURL: '");
                if (baseResultURL != null) 
                    json.append(out.encodeURL(baseResultURL));
                if (queryString != null)
                    json.append(queryString).append('&');
                json.append("op=Validate' }");

//                 System.err.println("Validation results are available at " +
//                                    baseResultURL + "op=Validate");

                out.setContentType(getContentTypeForFormat("json"));
                PrintWriter client = out.getWriter();
                client.println(json.toString());
                client.close();
            }
            else {
                // send results now 
                out.setContentType(getContentTypeForFormat(format));
                Writer client = out.getWriter();
                printer.transform(new DOMSource(results), 
                                  new StreamResult(client));
                client.close();
                done = true;
            }
        }
        catch (Exception ex) {
            log("failed to write results: " + ex);
            throw new InternalServerException(ex);
        }
    }

    /**
     * write a message to standard error so that it will get recorded in the
     * servlet server's log
     */
    public void log(String message) {
        StringBuffer sb = 
            new StringBuffer(DateFormat.getDateInstance().format(new Date()));
        sb.append(": ").append(serviceType).append(" baseURL=").append(baseURL);
        sb.append("\n  ").append(message);
        System.err.println(sb.toString());
    }

    /**
     * actually run the validation with the current set of arguments.  The 
     * caller wil handle responding with a successful response.
     * @param props   the input arguments
     * @param error   the response channel to use if an error occurs
     * @return Document  the results of the validation 
     */
    protected Document validate(Properties props, HttpServletResponse error) 
        throws ValidationException, IOException
    {
        Properties errors = new Properties();
        
        if (! checkInputs(errors)) {
            reportErrors(errors, props.getProperty("errorFormat"), error);
            return null;
        }
        else if (! available) {
            log(serviceType + " validater unavailable for unknown reasons");
            throw new UnavailableSessionException("validater is in an odd " +
                                           "state; please restart application");
        }

        try {
            HTTPGetTestQuery.QueryData[] qdl = null;
            if (userquery != null && userquery.length() > 0) {
                HTTPGetTestQuery.QueryData qd = makeQueryData(userquery);
//                     new HTTPGetTestQuery.QueryData("user", "user", userquery,
//                                                    "user-provided cone");
                qdl = new HTTPGetTestQuery.QueryData[] { qd };
            }

            return validater.validate(baseURL, qdl, this);
        }
        catch (MalformedURLException ex) { throw new BadRequestException(ex); }
        catch (Exception ex) { 
            log("validater failure: " + ex.getMessage());
//             ex.printStackTrace();
            throw new InternalServerException(ex);
        }
    }

    /**
     * wrap a user-provided service query in a QueryData object.  
     */
    protected HTTPGetTestQuery.QueryData makeQueryData(String query) {
        return new HTTPGetTestQuery.QueryData("user", "user", userquery,
                                              "user-provided query");

    }

    public void end(boolean asap) {
        validater.interrupt();
        super.end(asap);

        // reset our results
        results = null;
        done = false;
        progress.removeAllElements();
    }

    public void doCancel(Properties props, HttpServletResponse out)
         throws ValidationException, IOException
    {
        end(false);

        StringBuffer sb = new StringBuffer("{ 'status': ");
        if (! available) {
            sb.append("'unavailable'");
        }
        else if (validater.isValidating()) {
            sb.append("'running'");
        }
        else {
            sb.append("'done'");
        }
        sb.append(", 'message': 'validation interrupt message sent' }");

        out.setContentType(getContentTypeForFormat("json"));
        PrintWriter client = out.getWriter();
        client.println(sb.toString());
        client.close();
    }

    public void doGetStatus(Properties props, HttpServletResponse out)
         throws ValidationException, IOException
    {
        out.setContentType(getContentTypeForFormat("json"));
        PrintWriter client = out.getWriter();
        client.println(statusmsg);
        client.close();
    }

    public void doStartSession(Properties props, HttpServletResponse out)
         throws ValidationException, IOException
    {
        String baseResultURL = props.getProperty("validaterBaseURL");
        String queryString = props.getProperty("queryString");

        StringBuffer json = new StringBuffer("{ status: 'ready', sessionURL: '");
        if (baseResultURL != null) json.append(out.encodeURL(baseResultURL));
        if (queryString != null) json.append(queryString).append('&');
        json.append("' }");

        out.setContentType(getContentTypeForFormat("json"));
        PrintWriter client = out.getWriter();
        client.println(json.toString());
        client.close();
    }

    protected String getContentTypeForFormat(String format) {
        return ctypes.getProperty(format);
    }

    protected Transformer getTransformerForFormat(String format) 
         throws FileNotFoundException, IOException,
                TransformerConfigurationException
    {
        if (format != null && format.length() == 0) format = null;
        if (format == null) return null;

        Templates stylesheet = (Templates) templates.get(format);
        if (stylesheet == null) {
            String ssfile = config.getParameter("resultStylesheet", "format",
                                                format); 
            if (ssfile == null && "xml".equals(format)) ssfile = "";
            if (ssfile == null) return null;

            if (ssfile.length() == 0) return tf.newTransformer();

            stylesheet = tf.newTemplates(
               new StreamSource(Configuration.openFile(ssfile, this.getClass()))
            );
        }
            
        return stylesheet.newTransformer();
    }

}
