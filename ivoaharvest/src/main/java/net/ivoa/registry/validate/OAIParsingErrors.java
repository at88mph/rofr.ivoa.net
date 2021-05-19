package net.ivoa.registry.validate;

import org.nvo.service.validation.ParsingErrors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.DOMException;

/**
 * a ParsingErrors object for producing test failure elements while parsing
 * OAI query responses.
 */
public class OAIParsingErrors extends ParsingErrors {
    private boolean dopass = false;
    private Element parent = null;

    private static final String validdesc = 
        "OAI response must be a schema-compliant XML document";
    private static final String seeindivid = 
        "see results for individual Resource Records for more details";

    /**
     * create the handler 
     */
    public OAIParsingErrors() {
        itemLabel = "OAIvalid";
    }

    /**
     * create the handler 
     * @param dopass    if true, include a passing test element, if applicable
     */
    public OAIParsingErrors(boolean dopass) {
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

    public void insertErrors(Element parent, Node before, Node delim) 
        throws DOMException
    {
        this.parent = parent;
        super.insertErrors(parent, before, delim);
    }

    protected Element[] makeErrorElements(Document owner) {

        // first concatonate the error messages, but look for an STC 
        // ID error
        String[] stciderr = null;
        StringBuffer sb = new StringBuffer();
        for(int i=0; i < errors.size(); i++) {
            String[] err = (String[]) errors.elementAt(i);
            sb.append(err[0]).append(": ").append(err[2]);
            if (i < errors.size()-1) sb.append("\n");

            // cvc-id.2 indicates that an XML ID attribute is not unique.  This 
            // is typically because STC IDs are being reused.  
            if (err[2].indexOf("cvc-id.2") >= 0) stciderr = err;
        }
        String valerrs = sb.toString();

        int size = 0;            // the number of output elements we'll produce
        if (dopass || hasErrors()) size++;
        if (hasErrors()) size++;
        if (stciderr != null) size++;     // note: hasErrors() will return true
        if (size == 0) return new Element[0];
        Element[] out = new Element[size];

        // when errors are encountered, the first error element will indicate 
        // in general that parsing errors occurred.  
        out[0] = owner.createElement(testElem);
        out[0].setAttribute("status", (hasErrors()) ? "fail" : "pass");
        if (out.length > 1) out[1] = (Element) out[0].cloneNode(true);
        if (out.length > 2) out[2] = (Element) out[0].cloneNode(true);
        out[0].setAttribute("item", itemLabel);

        // provide a general message and append the element
        sb = new StringBuffer(validdesc);
        if (parent != null && 
            "ListRecords".equals(parent.getAttribute("name"))) 
        {
            sb.append(";\n").append(seeindivid);
        }
        sb.append(".");
        out[0].appendChild(owner.createTextNode(sb.toString()));

        // now actually list the specific parsing errors encountered
        if (out.length > 1) {
            sb = new StringBuffer();
            out[1].setAttribute("item", itemLabel + "-msgs");
            sb.append("Some schema validation errors encountered");
            if (parent != null && 
                parent.getAttribute("name").equals("ListRecords")) 
            {
                sb.append(";\n").append(seeindivid);
            }
            sb.append(".\n").append(valerrs);

            out[1].appendChild(owner.createTextNode(sb.toString()));
        }

        // if we discovered an STC ID problem, add a special element for 
        // this too.
        if (out.length > 2) {
            // shouldn't be necessary...but just in case
            if (stciderr == null) 
                stciderr = new String[] { null, null, "null" };

            out[2].setAttribute("item", itemLabel + "-cvc-id.2");
            out[2].appendChild(owner.createTextNode(stciderr[2]));
        }

        return out;
    }
}
