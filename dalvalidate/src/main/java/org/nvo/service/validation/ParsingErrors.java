package org.nvo.service.validation;

import java.util.Vector;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.DOMException;

/**
 * An Error Handler that captures parsing errors so that they can be 
 * reported later.  
 */
public class ParsingErrors implements ErrorHandler {

    protected String itemLabel = "parsing";

    /**
     * the container that collects errors.  
     * 
     * Each element is a 3-element String array where the first element is
     * the type and the second is the error message.  
     */
    protected Vector errors = new Vector();

    /**
     * the name to use for the test elements this object will create
     */
    protected String testElem = "test";

    /**
     * A label for non-fatal parsing errors
     */
    public final static String ERROR = "Error";

    /**
     * A label for non-fatal parsing errors
     */
    public final static String FATAL = "Fatal";

    /**
     * A label for non-fatal parsing errors
     */
    public final static String WARN = "Warning";


    /**
     * create a ParsingErrors object
     */
    public ParsingErrors() { }

    /**
     * set the name that should be used when creating error elements.
     */
    public void setTestElementName(String name) { testElem = name; }

    /**
     * return the name that will be used when creating error elements.
     */
    public String getTestElementName() { return testElem; }

    /**
     * return the item label to use for a error.  This 
     * implementation always returns the itemLabel property
     */
    public String getItemFor(SAXParseException ex) {
        return itemLabel;
    }

    /**
     * add an error message
     * @param type     the error type label (one of ERROR, WARN, or FATAL).
     * @param message  the error message
     */
    public void addError(String type, String label, String message) {
        String[] add = { type, label, message };
        errors.addElement(add);
    }

    /**
     * add an error message
     * @param type     the error type label (one of ERROR, WARN, or FATAL).
     * @param ex       the exception the resulted form the error
     */
    public void addError(String type, SAXParseException ex) {
        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
        }
        StringBuffer msg = new StringBuffer();
        if (systemId != null) msg.append(systemId).append(":");
        msg.append(ex.getLineNumber()).append(": ").append(ex.getMessage());
        addError(type, getItemFor(ex), msg.toString());
    }

    /**
     * record a warning.  This is called by the parser via the ErrorHandler
     * interface. 
     */
    public void warning(SAXParseException ex) {
        addError(WARN, ex);
        // ex.printStackTrace();
    }

    /**
     * record a non-fatal error.  This is called by the parser via the 
     * ErrorHandler interface. 
     */
    public void error(SAXParseException ex) {
        addError(ERROR, ex);
    }

    /**
     * record a fatal error.  This is called by the parser via the 
     * ErrorHandler interface. 
     */
    public void fatalError(SAXParseException ex) 
         throws SAXParseException
    {
        addError(FATAL, ex);
        throw ex;
    }

    /**
     * return true if errors have been recorded.
     */
    public boolean hasErrors() {
        return (errors.size() > 0);
    }

    /**
     * remove all the currently saved errors so that the handler can be 
     * reused
     */
    public void clear() {
        errors.removeAllElements();
    }

    /**
     * return a list of Elements for the validation results document describing
     * the encountered errors.  Subclasses can override this method to control 
     * how errors are mapped into elements.
     * @param owner   the owner document to use to create the elements.  
     */
    protected Element[] makeErrorElements(Document owner) {
        Element[] out = new Element[errors.size()];
        Element el = null;
        String[] val = null;
        String status = null;
        for(int i=0; i < out.length; i++) {
            val = (String[]) errors.elementAt(i);
            status = (val[0].equals(WARN)) ? "warn" : "fail";
            if (val[2].endsWith("\n"))
                val[2] = val[1].substring(0,val[1].length()-1);

            el = owner.createElement(testElem);
            el.setAttribute("item", val[1]);
            el.setAttribute("status", status);
            el.appendChild(owner.createTextNode(val[2]));

            out[i] = el;
        }

        return out;
    }

    /**
     * insert the list of errors into an element.  This method will call
     * makeErrorElements() to get the errors to be added and insert each one 
     * in order prior to the specified child.  
     * @param parent   the parent element
     * @param before   the child node to insert before.  This node must be a 
     *                    current child of the parent.
     * @param delim    if not null, a copy of this node should be inserted 
     *                    before each error element added.  This node must
     *                    be owned by the parent's document (i.e. its owner 
     *                    Document was used to created it).
     */
    public void insertErrors(Element parent, Node before, Node delim) 
        throws DOMException
    {
        Element[] list = makeErrorElements(parent.getOwnerDocument());

        for(int i=0; i < list.length; i++) {
            if (delim != null) 
                parent.insertBefore(delim.cloneNode(true), before);
            parent.insertBefore(list[i], before);
        }
    }

    /**
     * insert the list of errors into an element.  This method will call
     * makeErrorElements() to get the errors to be added and insert each one 
     * in order prior to the specified child.  
     * @param parent   the parent element
     * @param before   the child node to insert before
     */
    public void insertErrors(Element parent, Node before) 
        throws DOMException
    {
        insertErrors(parent, before, null);
    }

    /**
     * append the list of errors into an element.  This method will call
     * makeErrorElements() to get the errors to be added and append each one 
     * in order to the specified parent element.  If delim is specified, this 
     * method will actually insert its elements after the last non-Text node 
     * of the parent.
     * @param parent   the parent element
     * @param delim    if not null, a copy of this node should be appended
     *                    prior to each error element added.  This node must
     *                    be owned by the parent's document (i.e. its owner 
     *                    Document was used to created it).
     */
    public void appendErrors(Element parent, Node delim) 
        throws DOMException
    {
        if (delim == null) {
            appendErrors(parent);
            return;
        }
        
        Element[] list = makeErrorElements(parent.getOwnerDocument());

        // find the last non-Text element
        Node child = parent.getLastChild();
        while (child != null && child.getNodeType() != Node.TEXT_NODE) {
            child = child.getPreviousSibling();
        }
        if (child == null) child = parent.getFirstChild();

        if (child == null) {
            for(int i=0; i < list.length; i++) {
                parent.appendChild(list[i]);
                if (i < list.length-1) 
                    parent.appendChild(delim.cloneNode(true));
            }
        }
        else {
            for(int i=0; i < list.length; i++) {
                parent.insertBefore(delim.cloneNode(true), child);
                parent.insertBefore(list[i], child);
            }
        }
        
    }

    /**
     * append the list of errors into an element.  This method will call
     * makeErrorElements() to get the errors to be added and append each one 
     * in order to the specified parent element.  
     * @param parent   the parent element
     */
    public void appendErrors(Element parent) 
        throws DOMException
    {
        if (!hasErrors()) return;
        Element[] list = makeErrorElements(parent.getOwnerDocument());
        
        for(int i=0; i < list.length; i++) {
            parent.appendChild(list[i]);
        }
    }

}
