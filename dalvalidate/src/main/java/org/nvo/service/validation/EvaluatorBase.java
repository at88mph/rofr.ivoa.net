package org.nvo.service.validation;

import java.util.Properties;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.IOException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.DOMException;

/**
 * an abstract base class for Evaluator implementations.
 *
 * This implementation provides:
 * <ul>
 *   <li> setting and storage of properties </li>
 * </ul>
 */
public abstract class EvaluatorBase implements Evaluator {

    /**
     * properties to be used to configure the evaluation process.  The 
     * specific properties are generally implementation-specific.  At this
     * time, there are no property names considered standard across all 
     * implementations.
     */
    protected Properties evalprops = null;

    /**
     * create the base evaluator
     */
    public EvaluatorBase() {
        this(null);
    }

    /**
     * create the base evaluator, initialized with a set of properties
     * @param props   the properties to initially load.  A copy of the 
     *                  contents will be made.
     */
    public EvaluatorBase(Properties props) {
        if (props == null) 
            evalprops = new Properties();
        else 
            evalprops = (Properties) props.clone();
    }

    /**
     * set an evaluator property
     */
    public void setProperty(String name, String value) {
        evalprops.setProperty(name, value);
    }

    /**
     * get an evaluator property
     */
    public String getProperty(String name, String defaultVal) {
        return evalprops.getProperty(name, defaultVal);
    }

    /**
     * get an evaluator property
     */
    public String getProperty(String name) {
        return evalprops.getProperty(name);
    }

    /**
     * remove a property
     */
    public String removeProperty(String name) {
        try {
            return (String) evalprops.remove(name);
        }
        catch (ClassCastException ex) {
            return null;
        }
    }

    /**
     * remove all the properties
     */
    public void removeAllProperties() {
        Enumeration keys = evalprops.keys();
        while (keys.hasMoreElements())
            evalprops.remove(keys.nextElement());
    }

    /**
     * get the input stream from an invocation of a TestQuery.  This will 
     * ensure that the remote service is responding before returning.  If 
     * it takes too long, a TimeoutException is thrown.  Note that the 
     * timeout period will be gotten via {@link TestQuery#getTimeout}.  
     */
    public static InputStream getStream(TestQuery tq) 
        throws TimeoutException, IOException, InterruptedException
    {
        QueryConnection conn = tq.invoke();
        long timeout = tq.getTimeout();
        if (timeout < 0) timeout = 600000;  // ten minutes
        if (timeout == 0) timeout = 3600000;  // not quite forever (1 hour)

        if (! conn.waitUntilReady(timeout)) {
            try {
                conn.shutdown();
            } catch (IOException ex) {  /* ignore this */ }
            throw new TimeoutException(timeout);
        }

        return conn.getStream();
    }

    /**
     * return the first child element of a given Element
     */
    public static Element getFirstChildElement(Element parent) 
        throws DOMException
    {
        Node child = parent.getFirstChild();
        while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
            child = child.getNextSibling();
        }
        return (Element) child;
    }

    /**
     * return the last child element of a given Element
     */
    public static Element getLastChildElement(Element parent) 
        throws DOMException
    {
        Node child = parent.getLastChild();
        while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
            child = child.getPreviousSibling();
        }
        return (Element) child;
    }
}
