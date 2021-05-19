package org.nvo.service.validation.webapp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Date;
import java.text.DateFormat;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/** 
 * an HTTPServlet for validating a remote HTTP Get service through an 
 * AJAX-based interaction model.  
 * 
 * This servlet implementation manages {@link ValidationSession 
 * ValidationSession} objects, one for each user validation request.  The 
 * session class provides all the logic for invoking a validater and 
 * delivering information to the client.  The user client sends various
 * requests through the same servlet endpoint.  Each request encodes in its
 * arguments an operation name that should be invoked.  This implementation
 * is responsible for parsing the arguments to the operation and passing 
 * them to the session object.  
 *
 * This implementation uses the HttpSession to connect a user to a 
 * ValidationSession object.  It assumes that the endpoint is provided via
 * a query argument called "baseURL".  If it fails to find this parameter,
 * it will also look for a parameter called "errorFormat" to be used as a
 * hint in formatting an error response.  All other expected parameters are 
 * the choice of the subclass and the ValidationSession class it employs. 
 */
public abstract class ValidaterWebApp extends HttpServlet {

    /**
     * create the servlet
     */
    public ValidaterWebApp() { super(); }

    /**
     * parse the URL Get arguments into key-value pairs.  This method will 
     * look for a special argument called "runid" representing an identifier 
     * for a particular validation request; the value of this argument is 
     * returned.  If the argument is not set, null is returned.  This will
     * also set a "queryString" property which is the input querystring with
     * the runid and op parameters removed.  
     * @param querystring   the query string--everything after the ? in the 
     *                       GET URL--to parse.
     * @param out           the properties object to put the results into
     */
    public String parseArgs(String querystring, Properties out) {
        String name = null, value = null, old = null;
        String runid = null;
        int eq;
        StringTokenizer st = new StringTokenizer(querystring, "&");
        StringBuffer outqs = new StringBuffer();
        while (st.hasMoreTokens()) {
            String arg = st.nextToken();
            eq = arg.indexOf("=");
            if (eq > 0 && eq < arg.length()-1) {
                name = decode(arg.substring(0,eq).trim());
                value = decode(arg.substring(eq+1).trim());
                if (name.equals("runid")) {
                    runid = value;
                    out.setProperty(name, value);
                }
                else {
                    if (! name.equals("op")) {
                        outqs.append(name).append('=');
                        outqs.append(encode(value)).append('&');
                    }
                    old = out.getProperty(name);
                    if (old != null)
                        out.setProperty(name, old + " " + value);
                    else 
                        out.setProperty(name, value);
                }
            }
        }
        if (outqs.length() > 0) outqs.deleteCharAt(outqs.length()-1);
        out.setProperty("queryString", outqs.toString());

        return runid;
    }

    private String decode(String in) {
        try {
            return URLDecoder.decode(in, "UTF-8");
        }
        catch (UnsupportedEncodingException ex) { return in; }
    }

    private String encode(String in) {
        try {
            return URLEncoder.encode(in, "UTF-8");
        }
        catch (UnsupportedEncodingException ex) { return in; }
    }

    public void reportError(ValidationSession sess, String name, String message,
                            String format, HttpServletResponse resp) 
         throws InternalServerException, IOException
    {
        Properties errors = new Properties();
        errors.setProperty(name, message);
        sess.reportErrors(errors, format, resp);
    }

    /**
     * a factory method for creating a new ValidationSession object.
     */
    protected abstract ValidationSession newValidationSession()
        throws ValidationException;

    /**
     * handle a GET request.  If the URL arguments include a runid parameter,
     * this implementation will attempt to retrieve a cached ValidationSession 
     * object (which handles the real work) or will otherwise create and 
     * intialize a new one.  The parameters sent include not only the 
     * parameters passed in from the user but also:
     * <pre>
     *   runid                the current runid for the request
     *   validaterBaseURL     the modified base URL for this service that 
     *                          the (ajax) client should use to remain connected
     *                          to its session.  The URL may include a session
     *                          id (in the absense of cookies, set by the 
     *                          servlet engine) and the runid.
     *   queryString          the full query string that the client's parameters 
     *                          were parsed from, excluding the runid parameter.
     * </pre>
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
         throws ServletException, IOException
    {
        Properties args = new Properties();
        String qs = req.getQueryString();
        try {
            if (qs == null) 
                throw new ServletException("Missing arguments");
            String runid = parseArgs(qs, args);
            String op = args.getProperty("op");

            HttpSession session = req.getSession();
            ValidationSession validation = null;
//             System.err.println("request received from session, " + session);
            synchronized (session) {
                if (runid != null) {
                    validation = (ValidationSession) 
                        session.getAttribute("valsess" + runid);
                }

                if (validation == null) {
                    validation = newValidationSession();
                    String endpoint = args.getProperty("endpoint");
                    if (endpoint == null) {
                        reportError(validation, "endpoint", 
                                    "Please provide the URL for the service " +
                                    "to be validated", 
                                    args.getProperty("errorFormat"), resp);
                        return;
                    }
                    runid = validation.initialize(endpoint, args);
                    if (runid == null) throw new 
                        NullPointerException("Null runid returned from session");
                    if (runid.length() == 0) 
                        throw new 
                            IllegalArgumentException("Empty runid returned " + 
                                                     "from session");
                    session.setAttribute("valsess"+runid, validation);

                    System.err.println("Starting " + validation.getServiceType()
                                       + " validation session with " + qs);
                }
//              else {
//                  System.err.println("Resuming " + validation.getServiceType()
//                                     + "session " + runid + " with " + qs);
//              }
            }

            // set the base URL that future calls to this servlet will need
            // to preserve state. 
            args.setProperty("validaterBaseURL", 
                             resp.encodeURL(req.getRequestURL().toString()) + 
                             "?runid=" + runid + "&");

            boolean done = false;
            if (op == null || op.length() == 0) {
                done = validation.invokeDefaultOp(args, resp);
            }
            else {
                done = validation.invokeOp(op, args, resp);
            }
            if (done) {
                System.err.println("deleting session with runid="+runid);
                validation.end(false);
                session.setAttribute("valsess"+runid, null);
                validation = null;
            }
        }
        catch (UnavailableSessionException ex) {
            logProblem(ex, qs);
            resp.sendError(resp.SC_CONFLICT, ex.getMessage());
        }
        catch (BadRequestException ex) {
            logProblem(ex, qs);
            resp.sendError(resp.SC_BAD_REQUEST, ex.getMessage());
        }
        catch (ValidationException ex) {
            logProblem(ex, qs);
            resp.sendError(resp.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    /**
     * write error messages to standard error (so that they get logged).
     * @param ex           the exception that describes the problem
     * @param queryString  servlet arguments
     */
    protected void logProblem(ValidationException ex, String queryString) {
        String date = DateFormat.getDateInstance().format(new Date());
        System.err.println(date + ": " + ex.getMessage());
        System.err.println("  args: " + queryString);
        Exception orig = ex.getWrapped();
        while (orig != null && ex != orig && 
               orig instanceof ValidationException) 
        {
            ex = (ValidationException) orig;
            orig = ex.getWrapped();
        }
        if (orig == null) orig = ex;
        orig.printStackTrace();
    }
}
