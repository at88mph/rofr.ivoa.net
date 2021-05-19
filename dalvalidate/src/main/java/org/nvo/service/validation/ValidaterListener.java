package org.nvo.service.validation;

import java.util.Map;

/**
 * an interface for listening to the progress of a validater.  
 *
 * The listener's progressUpdated() method will be called whenever there's a 
 * change in the status of the validation.  The Map object passed will 
 * contain information regarding the status; the keys will always be strings,
 * but the values can be of any type.  All Validaters will provide
 * at least the following items in the status object:
 * <pre>
 * Key Name          Value Type  Description
 * ----------------  ----------  ------------------------------------------
 * id                String      the id representing the particular 
 *                                  validation request.  This is the same 
 *                                  as the id provided in the id argument.
 * done              Boolean     false if the validation process is still 
 *                                  running; true if it is done.  This is 
 *                                  the same value provided directly via the
 *                                  done argument
 * ok                Boolean     false if a fatal exception was thrown; true, 
 *                                  otherwise.
 * message           String      a user-oriented explanation of the latest 
 *                                  change in progress.  This may be an error
 *                                  message if "ok" is false.
 * exception         String      when applicable, the exception that was thrown
 *                                  causing "ok" to be false.
 * nextQueryName     String      a name given to next query to be sent.
 * nextQueryDescription  String  a description of the next query to be sent.
 * lastQueryName     String      a name given to last query completed
 * lastQueryDescription  String  a description of the last query completed.
 * queryTestCount    Integer     the number of tests applied in the last 
 *                                  completed query.
 * queryCount        Integer     the number of queries completed so far
 * totalQueryCount   Integer     the total number of queries expected to be 
 *                                  sent
 * totalTestCount    Integer     the total number of tests applied so far.  
 * </pre>
 *
 * Some implementations may choose to provide additional information.
 *
 * Note that the Validater may choose to send an update message prior to 
 * executing the first query.  In this case, the "queryName" and 
 * "queryDescription" will be empty strings.  
 *
 * As it is typical for a Validater to send progress to the listener in the 
 * same thread as the one carrying out the actual validation, it is recommended
 * that the implementation of this interface be kept lightweight so as not 
 * to slow down the validation.  
 */
public interface ValidaterListener {

    /**
     * signal a change in the status of the validation of interest.  Listeners
     * implement this method to receive updates about the validation progress.
     * @param id       a unique string representing the validation process 
     *                   being monitored.
     * @param done     true if the validation process has completed (either 
     *                   successfully or with an error), or false if the 
     *                   validation is still underway.  If true, this method
     *                   will not be called again for this validation with 
     *                   the given id.  
     * @param status   a set of properties indicating the status of the 
     *                   validation.  
     */
    public void progressUpdated(String id, boolean done, Map status);

}
