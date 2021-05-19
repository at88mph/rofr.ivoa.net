package ncsa.xml.saxfilter;

import ncsa.xml.sax.Namespaces;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

/**
 * a {@link SAXFilterContentHandler} that will pass events onto a sequence 
 * of ContentHandlers.
 */
public class MultiSFContentHandler 
    implements SAXFilterContentHandler, Iterable<ContentHandler>
{
    private LinkedList<ContentHandler> delegates = 
        new LinkedList<ContentHandler>();
    LinkedList<OnDemandParser> odpdels = new LinkedList<OnDemandParser>();
    OnDemandParser parent = null;

    /**
     * instantiate an empty ssquence of handlers
     */
    public MultiSFContentHandler() { }

    /**
     * instantiate a new sequence of handlers with one handler
     */
    public MultiSFContentHandler(ContentHandler handler) { 
        this();
        delegates.add(handler);
    }

    /**
     * add a ContentHandler to the internal list of delegates
     */
    public void addHandler(ContentHandler handler) {
        delegates.add(handler);
    }

    /**
     * provide an iterator to the currently added ContentHandlers
     */
    public Iterator<ContentHandler> iterator() { return delegates.iterator(); }

    /**
     * receive an interface for controlling parsing
     */
    public void setParseRequestMgr(OnDemandParser prm) {
        parent = prm;
        OnDemandParser del = null;
        for(ContentHandler ch : delegates) {
            if (ch instanceof SAXFilterContentHandler) {
                del = new ODPDelegate();
                odpdels.add(del);
                ((SAXFilterContentHandler) ch).setParseRequestMgr(del);
            }
        }
    }

    /**
     * receive an interface for controlling content flow
     */
    public void setFlowController(SAXFilterFlowControl control) {
        for(ContentHandler ch : delegates) {
            if (ch instanceof SAXFilterContentHandler)
                ((SAXFilterContentHandler) ch).setFlowController(control);
        }
    }

    /**
     * recieve a namespace tracker
     */
    public void setNamespaces(Namespaces namespaces) {
        for(ContentHandler ch : delegates) {
            if (ch instanceof SAXFilterContentHandler)
                ((SAXFilterContentHandler) ch).setNamespaces(namespaces);
        }
    }

    /**
     * Receive a Locator object for document events.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass if they wish to store the locator for use
     * with other document events.</p>
     *
     * @param locator A locator for all SAX document events.
     * @see org.xml.sax.ContentHandler#setDocumentLocator
     * @see org.xml.sax.Locator
     */
    public void setDocumentLocator(Locator locator)
    {
        for(ContentHandler ch : delegates) 
            ch.setDocumentLocator(locator);
    }
    
    
    /**
     * Receive notification of the beginning of the document.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the beginning
     * of a document (such as allocating the root node of a tree or
     * creating an output file).</p>
     *
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startDocument
     */
    public void startDocument()
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.startDocument();
    }
    
    
    /**
     * Receive notification of the end of the document.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the end
     * of a document (such as finalising a tree or closing an output
     * file).</p>
     *
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#endDocument
     */
    public void endDocument()
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.endDocument();
    }


    /**
     * Receive notification of the start of a Namespace mapping.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the start of
     * each Namespace prefix scope (such as storing the prefix mapping).</p>
     *
     * @param prefix The Namespace prefix being declared.
     * @param uri The Namespace URI mapped to the prefix.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startPrefixMapping
     */
    public void startPrefixMapping(String prefix, String uri)
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.startPrefixMapping(prefix, uri);
    }


    /**
     * Receive notification of the end of a Namespace mapping.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the end of
     * each prefix mapping.</p>
     *
     * @param prefix The Namespace prefix being declared.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#endPrefixMapping
     */
    public void endPrefixMapping(String prefix)
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.endPrefixMapping(prefix);
    }
    
    
    /**
     * Receive notification of the start of an element.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the start of
     * each element (such as allocating a new tree node or writing
     * output to a file).</p>
     *
     * @param uri The Namespace URI, or the empty string if the
     *        element has no Namespace URI or if Namespace
     *        processing is not being performed.
     * @param localName The local name (without prefix), or the
     *        empty string if Namespace processing is not being
     *        performed.
     * @param qName The qualified name (with prefix), or the
     *        empty string if qualified names are not available.
     * @param attributes The attributes attached to the element.  If
     *        there are no attributes, it shall be an empty
     *        Attributes object.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startElement
     */
    public void startElement(String uri, String localName,
			     String qName, Attributes attributes)
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.startElement(uri, localName, qName, attributes);
    }
    
    
    /**
     * Receive notification of the end of an element.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the end of
     * each element (such as finalising a tree node or writing
     * output to a file).</p>
     *
     * @param uri The Namespace URI, or the empty string if the
     *        element has no Namespace URI or if Namespace
     *        processing is not being performed.
     * @param localName The local name (without prefix), or the
     *        empty string if Namespace processing is not being
     *        performed.
     * @param qName The qualified name (with prefix), or the
     *        empty string if qualified names are not available.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#endElement
     */
    public void endElement(String uri, String localName, String qName)
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.endElement(uri, localName, qName);
    }
    
    
    /**
     * Receive notification of character data inside an element.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method to take specific actions for each chunk of character data
     * (such as adding the data to a node or buffer, or printing it to
     * a file).</p>
     *
     * @param chars The characters.
     * @param start The start position in the character array.
     * @param length The number of characters to use from the
     *               character array.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#characters
     */
    public void characters(char chars[], int start, int length)
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.characters(chars, start, length);
    }
    
    
    /**
     * Receive notification of ignorable whitespace in element content.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method to take specific actions for each chunk of ignorable
     * whitespace (such as adding data to a node or buffer, or printing
     * it to a file).</p>
     *
     * @param chars The whitespace characters.
     * @param start The start position in the character array.
     * @param length The number of characters to use from the
     *               character array.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#ignorableWhitespace
     */
    public void ignorableWhitespace(char chars[], int start, int length)
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.ignorableWhitespace(chars, start, length);
    }
    
    
    /**
     * Receive notification of a processing instruction.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions for each
     * processing instruction, such as setting status variables or
     * invoking other methods.</p>
     *
     * @param target The processing instruction target.
     * @param data The processing instruction data, or null if
     *             none is supplied.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#processingInstruction
     */
    public void processingInstruction(String target, String data)
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.processingInstruction(target, data);
    }


    /**
     * Receive notification of a skipped entity.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions for each
     * processing instruction, such as setting status variables or
     * invoking other methods.</p>
     *
     * @param name The name of the skipped entity.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#processingInstruction
     */
    public void skippedEntity(String name)
	throws SAXException
    {
        for(ContentHandler ch : delegates) 
            ch.skippedEntity(name);
    }

    static int[] etypes = { OnDemandParser.CHARACTERS, OnDemandParser.DOCUMENT, 
                            OnDemandParser.ELEMENT,    OnDemandParser.ATTRIBUTES,
                            OnDemandParser.IGNORE_WHITE_SPACE, 
                            OnDemandParser.SKIPPED_ENTITY, 
                            OnDemandParser.PROC_INSTR,
                            OnDemandParser.NAMESPACES, 
                            OnDemandParser.PREFIX_MAPPING };
    class ODPDelegate extends OnDemandParserDelegate {
        public ODPDelegate() { super(); }
        public ODPDelegate(boolean enableAll) { super(enableAll); }
        public ODPDelegate(int events) { super(events); }
        public void disableEvents(int events) {
            super.disableEvents(events);
            for(int e : etypes) {
                // loop through the events included in the request
                if ((e & events) == 0) continue;
                for(OnDemandParser odp : odpdels) {
                    if (odp.isEnabled(e)) {
                        // if any of our handlers want this event, 
                        // don't disable it
                        events &= ~e;
                        break;
                    }
                }
            }
            if (events > 0) parent.disableEvents(events);
        }
        public void enableEvents(int events) {
            super.enableEvents(events);
            parent.enableEvents(events);
        }
        public void loadAttributes(String elname, Set attnames) {
            super.loadAttributes(elname, attnames);
            // this implementation will always load all attributes.
        }
    }
}

