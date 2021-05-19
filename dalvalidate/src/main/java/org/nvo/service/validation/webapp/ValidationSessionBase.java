package org.nvo.service.validation.webapp;

import org.nvo.service.validation.WrappedIOException;

import java.util.Properties;
import java.util.Hashtable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

/**
 * an abstract parent implementation of the 
 * {@link ValidationSession ValidationSession} interface. 
 * <p>
 * This parent class provides implementations for the HttpSessionListener
 * interface, for managing the availability state, and for invoking specific
 * operation methods.  
 */ 
public abstract class ValidationSessionBase implements ValidationSession {

    static public final String JSON_CONTENT_TYPE = "application/jsonrequest";

    static private int nextRequestIndex = 0;

    /**
     * the HTTP session associated with this validation session.
     */
    protected HttpSession hsess = null;

    /**
     * the current request identifier, as set by initialize()
     */
    protected String runid = null;

    /**
     * true if this session has been properly initialized and is in a useable
     * state.
     */
    protected boolean available = false;

    /**
     * true if this session can be considered as having completed its work
     */
    protected boolean done = false;

    /**
     * the name of the operation to call if one was not specified
     */
    protected String defaultOp = null;

    /**
     * a name for the type of service that is validated by this session.  This
     * is usually set by the specific implementation of this class via 
     * {@link #setServiceType(String) setServiceType()}.  
     */
    protected String serviceType = "DAL service";

    /**
     * create the session
     */
    public ValidationSessionBase() { }

    /**
     * save the HttpSession this validater is associated with.  This 
     * is called by the Servlet container when the session is created. 
     */
    public void sessionCreated(HttpSessionEvent ev) { hsess = ev.getSession(); }

    /**
     * save the HttpSession this validater is associated with.  This 
     * is called by the Servlet container when the session is created. 
     */
    public void sessionDestroyed(HttpSessionEvent ev) { 
        if (hsess == ev.getSession()) {
            end(true);
            hsess = null;
        }
    }

    /**
     * lookup the method to call for the given operation name.
     */
    protected abstract Method getOpMethod(String op);

    /**
     * load all of the operation methods in the given class into the given 
     * hashtable.  An operation method is one of the form 
     * "<code>do<i>XXX</i>(Properties, HttpServletResponse)</code>", where 
     * <code><i>XXX</i></code> is the name of the operation.  Implementations 
     * can use this to create a static lookup of operation methods to be used 
     * by {@link #getOpMethod(String) getOpMethod()}.  
     * @param vsclass    the class that will handle validation session 
     *                     operations
     * @param lookup     the hashtable to load methods into
     */
    public static void loadOpMethods(Class vsclass, Hashtable lookup) {
        Method[] methods = vsclass.getMethods();
        for(int i=0; i < methods.length; i++) {
            Class[] params = methods[i].getParameterTypes();
            if (methods[i].getName().startsWith("do") && params.length == 2
                && params[0].equals(Properties.class) 
                && params[1].equals(HttpServletResponse.class))
            {
                lookup.put(methods[i].getName().substring(2), methods[i]);
            }
        }
    }

    public static String correctBaseURL(String url) {
        if (url == null) return null;
        url = url.trim();
        char lastChar = url.charAt(url.length()-1);
        boolean hasQM = url.indexOf('?') > 0;
        if (hasQM && lastChar != '?' && lastChar != '&') 
            return url + '&';
        else if (!hasQM)
            return url + '?';
        else 
            return url;
    }

    protected static String newRequestID() {
        return "vsb" + nextRequestIndex++;
    }

    /**
     * set the type of service this session is configured to validate
     */
    public void setServiceType(String typeName) {
        serviceType = typeName;
    }

    /**
     * get the type of service this session is confibured to validate
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * initialize the session with an endpoint and parameters.  This will
     * typically be called immediately after construction (or from within
     * the constructor).  If this session is already in use when this 
     * method is called, it should either implicitly call {@link #end(boolean) 
     * end()} and reset the session to an initial state or throw an
     * UnavailableSessionException.  It is possible that the state of this
     * session is so corrupted that resetting the state is not possible; in 
     * this case, UnavailableSessionException should be thrown.
     *
     * This implementation sets an internal flag making this session available.
     * Subclasses should override this method to actually store the endpoint,
     * initialize, and then call super.initialize().
     *
     * @param endpoint    the endpoint of the service to be validated
     * @param params      input parameters to the validation session.  This
     *                       can be null when none are provided.
     * @exception UnavailableSessionException  if the session is in an 
     *                       unavailable state and cannot be reset and/or 
     *                       initialized.  
     * @return String     a unique identifier for the validation request being
     *                       handled by this ValidationSession
     */
    public String initialize(String endpoint, Properties params) 
         throws UnavailableSessionException 
    {
        available = true;
        if (runid == null) runid = newRequestID();
        return runid;
    }

    /**
     * invoke a named operation.
     * @param name    the name of the operation to invoke.  A null or empty
     *                   value should be interpreted as a request to execute
     *                   the validation process synchronously.
     * @param params  input parameters to the operation.  This can be null 
     *                   if none are required by the operation.
     * @param out     the servlet response object to write out to
     * @exception UnavailableSessionException  if the session is in an 
     *                   unavailable state, possible corrupted.  
     * @exception BadRequestException  if the operation request is invalid in
     *                   some way: either the operation name is not recognized
     *                   or the parameters are incorrect or insufficient.  An 
     *                   HTTP servlet would typically respond to this exception
     *                   with a 400 message.
     * @exception InternalServerException  if the validating server encounters
     *                   an internal error (by no fault of the user).  An 
     *                   HTTP servlet would typically respond to this exception
     *                   with a 500 message.
     * @exception IOException  if an I/O problem occurs while writing the 
     *                   output.
     * @return boolean   true if the session's work can be considered complete,
     *                   allowing this object to be discarded.  
     */
    public boolean invokeOp(String name, Properties params,
                            HttpServletResponse out)
         throws ValidationException, IOException 
    {
        Method doop = getOpMethod(name);
        if (doop == null) 
            throw new BadRequestException("Unknown operation: " + name);

        try {
            doop.invoke(this, new Object[] { params, out });
            return done;
        }
        catch (InvocationTargetException ex) {
            Throwable e = ex.getCause();
            if (e instanceof BadRequestException) 
                throw new BadRequestException((Exception) e);
            else if (e instanceof InternalServerException) 
                throw new InternalServerException((Exception) e);
            else if (e instanceof IOException) 
                throw new WrappedIOException((IOException) e);
            else if (e instanceof RuntimeException)
                throw ((RuntimeException) e);
            else if (e instanceof Error)
                throw ((Error) e);
            else 
                // this really shouldn't happen.  Maybe if we call it an
                // interanl server error, it will prompt the implementer to 
                // fix the code and not allow this exception
                throw new InternalServerException((Exception) e, 
                                              "unexpected checked exception");
        }
        catch (Exception ex) {
            throw new InternalError("programmer error during invokeOp: " + 
                                    ex.getMessage());
        }
    }

    /**
     * invoke the default operation.  The implementation usually calls 
     * {@link #invokeOp(String,Properties,HttpServletResponse) invokeOp()} 
     * with the appropriate operation name.
     * @param params  input parameters to the operation.  This can be null 
     *                   if none are required by the operation.
     * @param out     the servlet response object to write out to
     * @exception UnavailableSessionException  if the session is in an 
     *                   unavailable state, possible corrupted.  
     * @exception BadRequestException  if the operation request is invalid in
     *                   some way: either the operation name is not recognized
     *                   or the parameters are incorrect or insufficient.  An 
     *                   HTTP servlet would typically respond to this exception
     *                   with a 400 message.
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
        if (defaultOp == null)
            throw new BadRequestException("No default operation set; " +
                                          "operation name is required");
        return invokeOp(defaultOp, params, out);
    }

    /**
     * end the session.  Any call to 
     * {@link #invokeOp(String,Properties,HttpServletResponse) invokeOp()}
     * should throw an UnavailableSessionException exception until a call to 
     * {@link #initialize(String, Properties) initialize()} successfully 
     * resets the session.  
     * 
     * This implementation sets a flag indicating that the session is 
     * unavailable.  Subclasses should override this method to actually stop
     * its validaters and then call super.end().  
     *
     * @param asap   if true, interrupt any running validaters to shut them 
     *                 down as soon as possible; otherwise, just prevent 
     *                 further operation invocations until the session is 
     *                 reset via {@link #initialize(String,Properties) 
     *                 initialize()}.  
     */
    public void end(boolean asap) {
        available = false;
    }

    /**
     * return false if the Session state is reparably corrupted.  In this 
     * case, a call to 
     * {@link #invokeOp(String,Properties,HttpServletResponse) invokeOp()} 
     * is expected to throw an UnavailableSessionException.  The only 
     * possible way to continue using this session is to reset it with a 
     * call to {@link #initialize(String,Properties) initialize()} function 
     * (which could throw an UnavailableSessionException).  
     */
    public boolean isOK() { return available; }

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
     * @param format   the name of the format to be preferred in encoding the 
     *                    error message.  Generally, the allowed names are 
     *                    application-specific; however, they usually include
     *                    values like "html", "xml", "text", and/or "json".
     *                    This input is a hint; the implementation may choose
     *                    to ignore this.  
     * @param out      the response to write the error document out to.
     */
    public abstract void reportErrors(Properties errors, String format, 
                                      HttpServletResponse out)
         throws InternalServerException, IOException;

}
