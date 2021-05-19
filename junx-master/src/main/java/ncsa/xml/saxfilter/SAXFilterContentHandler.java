/*
 * NCSA BIMAgrid
 * Radio Astronomy Imaging Group
 * National Center for Supercomputing Applications
 * University of Illinois at Urbana-Champaign
 * 605 E. Springfield, Champaign IL 61820
 * rai@ncsa.uiuc.edu
 *
 *-------------------------------------------------------------------------
 * History: 
 *  2001/07/26  rlp  initial version
 *  2001/09/17  rlp  added setFlowController()
 *  2007/11/16  rlp  reconstructed from jad-decompiled class file
 */
package ncsa.xml.saxfilter;

import ncsa.xml.sax.Namespaces;
import org.xml.sax.ContentHandler;

/**
 * an XML SAX ContentHandler with extended capabilities for use with the 
 * SAXFilteredReader.  It provides methods that allow a content handler to 
 * control the parsing and flow of XML content through a SAXFilteredReader.
 * <p>
 * Like a ContentHandler (which it inherits from), these methods are called
 * by the parser to provide handler with controlling interfaces that it can use 
 * to affect the parsing and data flow.  These extension methods are called as
 * the parser is setting up, before any SAX event methods are called.  
 */
public interface SAXFilterContentHandler extends ContentHandler {

    /**
     * receive an interface for controlling parsing.
     * <p>
     * An {@link OnDemandParser} instance allows the handler to tell 
     * the parser what SAX events it should receive.  In particular, 
     * the handler can change what events it sees dyanamically while 
     * parsing is occuring.  
     * <p>
     * The purpose of this feature is performance: by telling the parser 
     * what SAX events it needs (and indirectly, what it doesn't need),
     * the parser can skip certain parsing tasks and run faster.
     */
    public void setParseRequestMgr(OnDemandParser prm);

    /**
     * receive an interface for controlling content flow.
     * <p>
     * Given that the handler sees XML content (as it is parsed) before 
     * it is fed to the consumer via the filtered Reader,
     * a {@link SAXFilterFlowControl} instance gives the handler control 
     * over what characters actually appear in the filtered stream of 
     * raw characters based on what XML content it sees.  The handler can 
     * cause sections of the stream to be skipped/thrown away (e.g. remove 
     * a node from the XML stream), or it can alter the content, or it insert 
     * new content into the stream.  
     */
    public void setFlowController(SAXFilterFlowControl control);

    /**
     * provide a namespace tracker
     * <p>
     * As the handler examines the XML content being passed to the filtered
     * reader, it may need to keep track of namespaces.  A {@link Namespaces}
     * instance passed to this method is kept up to date by the parser.  By 
     * sharing this with the handler, the handler can know what namespaces 
     * are in in force without having to keep track of them directly via 
     * namespace SAX events.
     * <p>
     * This method is normally called before any SAX events are passed to
     * the handler.  
     */
    public void setNamespaces(Namespaces namespaces);
}
