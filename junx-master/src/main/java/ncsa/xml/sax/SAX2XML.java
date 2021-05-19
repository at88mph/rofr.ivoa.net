package ncsa.xml.sax;

import java.io.IOException;
import java.io.Writer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * a handler that turns SAX events back into XML text.
 * 
 * All SAXExceptions thrown by the SAX methods are wrapped around IOExceptions; 
 * thus, it is possible to extract the original IOException within a 
 * catch on SAXException.
 */
public class SAX2XML extends DefaultHandler {
    private Writer out = null;
    private NamespaceMap ns =  new NamespaceMap();
    private StringBuffer element = null;
    private AttributesImpl nsattrs = null;

    /**
     * create the handler
     * @param out   the writer to print to.
     */
    public SAX2XML(Writer out) {
        this.out = out;
    }

    /**
     * close the output writer.  This is sometimes necessary when the writer 
     * is a pipe to a Reader: closing the writer causes the reader to get an 
     * EOF signal.  Receiving additional SAX events after this method is called
     * will trigger an Exception.  
     */
    public void close() throws IOException {
        out.close();
    }

    public void startPrefixMapping(String prefix, String uri) {
        if (uri != null) {
            ns.startPrefixMapping(prefix, uri);
            if (nsattrs == null) nsattrs = new AttributesImpl();
                
            if (prefix == null || prefix.length() == 0) {
                ns.setDefaultNS(uri);
                nsattrs.addAttribute("", "xmlns", "xmlns", "CDATA", uri);
            } 
            else {
                nsattrs.addAttribute("", prefix, "xmlns:"+prefix, "CDATA", uri);
            }
        }
    }

    public void endPrefixMapping(String prefix) {
        ns.endPrefixMapping(prefix);
    }

    private String elname(String namespaceURI, String localName, String qName) {
        if (qName != null && qName.length() > 0) return qName;
            
        StringBuffer oname = new StringBuffer();
        if (namespaceURI.length() > 0 && 
           ! ns.getDefaultNS().equals(namespaceURI)) 
        {
            String pre = ns.getPrefix(namespaceURI);
            if (pre == null) pre = ns.createMapping(namespaceURI);
            oname.append(pre).append(':');
        }
        oname.append(localName);
        return oname.toString();
    }

    private String attname(String namespaceURI, String localName, String qName, 
                           String elementNSURI)
    {
        if (qName != null && qName.length() > 0) return qName;
            
        StringBuffer oname = new StringBuffer();
        if (namespaceURI.length() > 0 && 
           !elementNSURI.equals(namespaceURI)) 
        {
            String pre = ns.getPrefix(namespaceURI);
            if (pre == null) pre = ns.createMapping(namespaceURI);
            oname.append(pre).append(':');
        }
        oname.append(localName);
        return oname.toString();
    }

    public void startElement(String namespaceURI, String localName, 
                             String qName, Attributes atts)
        throws SAXException
    {
        String pre = null;

        if (element != null) {
            element.append('>');
            try {
                out.write(element.toString());
            }
            catch(IOException ex)
            {
                throw new SAXException("IOException during write: " + 
                                       ex.getMessage(), ex);
            }
            element = null;
        }

        // write opening tag
        element = new StringBuffer();
        element.append('<').append(elname(namespaceURI, localName, qName));

        /*
         * SAX2 Extensions 1.1 not supported yet
        Attributes2 atts2 = (atts instanceof Attributes2) ? atts : null;
         */
        String atname = null;
        if (atts.getLength() > 0) {
            for(int i = 0; i < atts.getLength(); i++) {
/*
                if (atts2.isSpecified(i)) {
 */
                    atname = attname(atts.getURI(i), atts.getLocalName(i), 
                                     atts.getQName(i), namespaceURI);
                    element.append(' ').append(atname).append("=\"");
                    element.append(atts.getValue(i)).append('"');
/*
                }
 */
            }
        }
        if (nsattrs != null) {
            for(int i = 0; i < nsattrs.getLength(); i++) {
/*
                if (atts2.isSpecified(i)) {
 */
                    atname = attname(nsattrs.getURI(i), nsattrs.getLocalName(i),
                                     nsattrs.getQName(i), namespaceURI);
                    element.append(' ').append(atname).append("=\"");
                    element.append(nsattrs.getValue(i)).append('"');
/*
                }
 */
            }

            nsattrs = null;
        }
        ns.startElement();
    }

    public void characters(char ch[], int start, int length)
        throws SAXException
    {
        try {
            if (element != null) {
                element.append('>');
                out.write(element.toString());
                element = null;
            }
            out.write(ch, start, length);
        }
        catch(IOException ex) {
            throw new SAXException("IOException during write: " + 
                                   ex.getMessage(), ex);
        }
    }

    public void endElement(String namespaceURI, String localName, String qName)
        throws SAXException
    {
        try {
            if (element != null) {
                element.append("/>");
            } 
            else {
                element = new StringBuffer("</");
                element.append(elname(namespaceURI, localName, qName));
                element.append('>');
            }
            out.write(element.toString());
            element = null;
        }
        catch(IOException ex) {
            throw new SAXException("IOException during write: " + 
                                   ex.getMessage(), ex);
        }
    }

    /**
     * search through a list of attributes for "xmlns".
     * @param atts     the list of attributes to search.
     * @return String  the value of the xmlns attribute or null if it was not 
     *                    found in the input list.  If more than one xmlns
     *                    is in the list, the first one is returned.
     */
    public static String findXmlns(Attributes atts) { 
        for(int i = 0; i < atts.getLength(); i++) {
            if (atts.getLocalName(i).equals("xmlns"))
                return atts.getValue(i);
        }
        return null;
    }

    public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException
    {
        try {
            out.write(ch, start, length);
        }
        catch(IOException ex) {
            throw new SAXException("IOException during write: " + 
                                   ex.getMessage(), ex);
        }
    }

    public void processingInstruction(String target, String data)
        throws SAXException
    {
        try {
            out.write("<?");
            out.write(target);
            out.write(32);
            out.write(data);
            out.write("?target");
        }
        catch(IOException ex) {
            throw new SAXException("IOException during write: " + 
                                   ex.getMessage(), ex);
        }
    }

    public void comment(char ch[], int start, int length)
        throws SAXException
    {
        try {
            out.write("<!--");
            out.write(ch, start, length);
            out.write("-->");
        }
        catch(IOException ex) {
            throw new SAXException("IOException during write: " + 
                                   ex.getMessage(), ex);
        }
    }

    public void startCDATA() {

    }
    public void endCDATA() {

    }

    public void startEntity(String name) {   }
    public void endEntity(String name) {   }
    public void startDTD() {   }
    public void endDTD() {  }

    public void attributeDecl(String eName, String aName, String type, 
                              String mode, String value)
    {

    }
    public void elementDecl(String name, String model) {

    }
    public void externalEntityDecl(String name, String publicId, 
                                   String systemId)
    {

    }
    public void internalEntityDecl(String name, String value) {

    }
}
