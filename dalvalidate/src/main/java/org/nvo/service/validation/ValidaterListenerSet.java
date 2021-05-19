package org.nvo.service.validation;

import java.util.Properties;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ListIterator;
import java.util.Collections;

/**
 * a set of ValidaterListeners.  
 *
 * The listener's progressUpdated() method will be called whenever there's a 
 * change in the status of the validation.  The Properties object passed will 
 * contain information regarding the status.  
 */
public class ValidaterListenerSet implements ValidaterListener {

    List chain = Collections.synchronizedList(new LinkedList());

    /**
     * signal a change in the status of the validation of interest.  Listeners
     * implement this method to receive updates about the validation progress.
     * @param id       a unique string representing the validation process 
     *                   being monitored.
     * @param status   a set of properties indicating the status of the 
     *                   validation.  
     * @param done     true if the validation process has completed (either 
     *                   successfully or with an error), or false if the 
     *                   validation is still underway.  If true, this method
     *                   will not be called again for this validation with 
     *                   the given id.  
     */
    public void progressUpdated(String id, boolean done, Map status) 
    {
        ValidaterListener l = null;
        synchronized (chain) {
            ListIterator it = chain.listIterator();
            while(it.hasNext()) {
                l = (ValidaterListener) it.next();
                l.progressUpdated(id, done, status);
            }
        }
    }

    /**
     * add a listener
     */
    public void addListener(ValidaterListener listener) {
        chain.add(listener);
    }

    /**
     * remove a listener
     * @return boolean   true if the listener was found and removed.
     */
    public boolean removeListener(ValidaterListener listener) {
        return chain.remove(listener);
    }

    /**
     * remove all listeners
     * @return int   the number of listeners returned.
     */
    public int removeAllListeners() {
        Object[] listeners = chain.toArray();
        int i = 0;
        for(i=0; i < listeners.length; i++) {
            chain.remove(listeners[i]);
        }
        return i;
    }

}
