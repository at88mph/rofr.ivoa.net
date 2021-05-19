package net.ivoa.registry.util;

import net.ivoa.registry.std.RIStandard;

import ncsa.xml.saxfilter.XMLStreamEditor;
import ncsa.xml.saxfilter.SAXFilterContentHandler;
import ncsa.xml.saxfilter.MultiSFContentHandler;

import java.io.Reader;
import java.io.FileReader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.xml.namespace.QName;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;

/**
 * A class for modifying VOResource records on-the-fly.  This can be applied 
 * to VOResource records in a variety of forms:
 * <ol>
 *  <li> a document containing a single VOResource records with no external 
 *       wrapper or envelope </li>
 *  <li> one or more VOResource records wrapped in an arbitrary external
 *       wrapper or envelope </li>
 *  <li> a sequence of separate VOResource documents. </li>
 * </ol>
 * It uses the Junx SAXFilteredReader and XMLStreamEditor classes to modify
 * the XML data on the fly.  Modules that edit the streams are attached in 
 * the form of SAXFilterContentHandler instances.  Consequently, to use this
 * class directly, one must be familier with the Junx saxfilter API.  Thus,
 * this class primarily serves as base class for specialized editors, such 
 * as the {@link VOResourceUpdater}.  
 * <p>
 */
public class VOResourceStreamEditor 
    extends XMLStreamEditor implements VOResourceRootAware 
{
    QName vorroot = null;

    /**
     * set the element to search to find the root of a VOResource record.
     * @param el   the qualified name of the VOResource root element.  An 
     *                example would 
     *                <code>RIStandard.resourceElementQName()</code>.
     */
    @Override public void setResourceRoot(QName el) {
        vorroot = el;
    }

    /**
     * set the element to search to find the root of a VOResource record.
     * @param nsuri     the element's namespace URI
     * @param elname    the element's localname
     */
    public void setResourceRoot(String nsuri, String elname) {
        setResourceRoot(new QName(nsuri, elname));
    }

    /**
     * set the root element to the Registry Interface standard element
     * as defined by the given standards properties.  
     * @param std   a RIProperties Properties instance that contains the 
     *                RI standard strings.  That is, a Properties instance
     *                returned by {@link RIStandard#getDefaultDefinitions()} or 
     *                {@link RIStandard#getDefintionsFor(String)}.  If null, 
     *                the Properties returned by 
     *                {@link #getDefaultDefinitions()} will be used.  
     */
    public void setResourceRoot(Properties std) {
        setResourceRoot(RIStandard.getRIResourceRoot(std));
    }

    /**
     * set the root element to the Registry Interface standard element
     * as defined by the given standards properties.  
     */
    public void setRIResourceRoot() {
        setResourceRoot(RIStandard.getRIResourceRoot());
    }

    /**
     * return the element to search to find the root of a VOResource record.
     * @return QName -- the qualified name of the VOResource root element or 
     *                  null if the document's root element is assumed to be 
     *                  the root of the VOResource record.
     */
    @Override public QName getResourceRoot() { return vorroot; }

    /**
     * return true if the XML document's root element is assumed to be the 
     * root of the VOResource record.  In other words, true means that the 
     * document contains a single VOResource record with no outer wrapper or 
     * envelope.  
     */
    @Override public boolean isVOResourceDoc() { return (vorroot == null); }


}