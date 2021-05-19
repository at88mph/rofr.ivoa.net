package org.nvo.service.validation.webapp;

import java.util.Properties;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionListener;

/**
 * an interface for managing a validation process across a conversation with 
 * the requesting client (i.e. a session).  
 * <p>
 * Validating a service can take a noticeable amount of time (by web 
 * standards); thus, an asynchronous model executing a validation request
 * can be helpful when the request comes from an interactive use via, 
 * typcially, a web page.  This interface captures a model for interacting 
 * with a client wishing to validate a service that can be used readily as
 * part of an AJAX-based session.  This model typically involves responding 
 * to following requests:
 * <ul>
 *   <li> starting the session by identifying the service to validate 
 *        by its endpoint and providing some overall validation constraints. 
 *        </li>
 *   <li> starting a validation process.  A session may split this up into 
 *        several, separately requestable parts; however, when only one 
 *        comprehensive request is provided, it will usually be combined 
 *        with session initiation.  </li>
 *   <li> requesting updates on the progress of the validation.  </li>
 *   <li> requesting results on validation results.  </li>
 *   <li> shutting down a session.  Even when a session may end naturally 
 *        when the results are delivered, this is needed to cancel requests
 *        that are taking too long.  </li>
 * </ul>
 * 
 * This interface provides a pattern for collecting all of the logic for 
 * managing a session and make it accessible via a single service endpoint 
 * (e.g. an HTTPServlet).  
 *
 * This interface assumes that a session ID that connects a ValidationSession 
 * instance to a user is handled outside of this interface (e.g. via an 
 * HttpSession).  
 * 
 * As an extension of HttpSessionListener, the implementation should be 
 * prepared for events indicating that the owning HttpSession is shutting
 * down.  Obviously, it should do so by calling end().
 */ 
public interface ValidationSession extends HttpSessionListener {

    /**
     * initialize the session with an endpoint and parameters.  This will
     * typically be called immediately after construction (e.g. by a 
     * ValidaterWebApp object).  If this session is already in use when this 
     * method is called, it should either implicitly call {@link #end(boolean) 
     * end()} and reset the session to an initial state or throw an
     * UnavailableSessionException.  It is possible that the state of this
     * session is so corrupted that resetting the state is not possible; in 
     * this case, UnavailableSessionException should be thrown.
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
         throws UnavailableSessionException;

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
         throws ValidationException, IOException;

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
         throws ValidationException, IOException;

    /**
     * end the session.  Any call to 
     * {@link #invokeOp(String,Properties,HttpServletResponse) invokeOp()}
     * should throw an UnavailableSessionException exception until a call to 
     * {@link #initialize(String, Properties) initialize()} successfully 
     * resets the session.  
     * @param asap   if true, interrupt any running validaters to shut them 
     *                 down as soon as possible; otherwise, just prevent 
     *                 further operation invocations until the session is 
     *                 reset via {@link #initialize(String,Properties) 
     *                 initialize()}.  
     */
    public void end(boolean asap);

    /**
     * return false if the Session state is reparably corrupted.  In this 
     * case, a call to 
     * {@link #invokeOp(String,Properties,HttpServletResponse) invokeOp()} 
     * is expected to throw an UnavailableSessionException.  The only 
     * possible way to continue using this session is to reset it with a 
     * call to {@link #initialize(String,Properties) initialize()} function 
     * (which could throw an UnavailableSessionException).  
     */
    public boolean isOK();

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
    public void reportErrors(Properties errors, String format, 
                             HttpServletResponse out)
         throws InternalServerException, IOException;

    /**
     * get the type of service this session is confibured to validate
     */
    public String getServiceType();

}
