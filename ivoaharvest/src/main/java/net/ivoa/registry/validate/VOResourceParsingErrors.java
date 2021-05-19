package net.ivoa.registry.validate;

import org.nvo.service.validation.ParsingErrors;

import org.xml.sax.SAXParseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.DOMException;

/**
 * a ParsingErrors object for producing test failure elements while parsing
 * individual VOResource documents.  
 */
public class VOResourceParsingErrors extends ParsingErrors {
    private boolean dopass = false;

    private static final String validdesc = 
        "Resource record must be compliant with the VOResource schemas.";

    /**
     * create the handler 
     */
    public VOResourceParsingErrors() { 
        itemLabel = "VR";
    }

    /**
     * create the handler 
     * @param dopass    if true, include a passing test element, if applicable
     */
    public VOResourceParsingErrors(boolean dopass) { 
        this();
        this.dopass = dopass; 
    }

    /**
     * set whether a test result element indicating passed tests will be 
     * included when applicable. 
     */
    public void setIncludePass(boolean yes) { dopass = yes; }

    /**
     * return true if a test result element indicating passed tests will be 
     * included when applicable. 
     */
    public boolean willIncludePass() { return dopass; }

    /**
     * return the item label to use for a error.  This implementation parses
     * the exception message for an error name.  
     */
    public String getItemFor(SAXParseException ex) {
        String label = super.getItemFor(ex);
        String msg = ex.getMessage();
        int colon = msg.indexOf(":");
        boolean aboutUndefPrefix = msg.startsWith("The prefix ");
        if (aboutUndefPrefix) {
            label = label + "-undefPrefix";
        }
        else if (colon > 0) {
            label = label + "-" + msg.substring(0,colon).trim();
        }
        return label;
    }

    protected Element[] makeErrorElements(Document owner) {
        int size = (dopass || hasErrors()) ? 1 : 0;
        size += errors.size();
        if (size == 0) return new Element[0];

        int nfail = 0, nwarn = 0;
        String status = null;

        Element[] out = new Element[size];
        for(int i=1; i < out.length; i++) {
            String[] err = (String[]) errors.elementAt(i-1);
            out[i] = owner.createElement(testElem);
            out[i].setAttribute("item", err[1]);
            if (WARN.equals(err[0])) {
                status = "warn";
                nwarn++;
            } else {
                status = "fail";
                nfail++;
            }
            out[i].setAttribute("status", status);

            String msg = err[2];
            if (msg.endsWith("\n")) msg = msg.substring(0,msg.length()-1);
            out[i].appendChild(owner.createTextNode(msg));
        }

        out[0] = owner.createElement(testElem);
        out[0].setAttribute("item", itemLabel + "valid");
        out[0].setAttribute("status", 
                            (nfail > 0) ? "fail" 
                                        : ((nwarn > 0) ? "warn" : "pass"));
        out[0].appendChild(owner.createTextNode(validdesc));

        return out;
    }

}


