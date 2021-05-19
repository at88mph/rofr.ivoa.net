package ncsa.xml.saxfilter;

import java.io.IOException;
import org.xml.sax.SAXException;

/**
 * an indication of an IOException during SAX processing.
 * <p>
 * With a {@link SAXFilterContentHandler} in which it is possible to 
 * insert data into the character stream, it is possible to encounter 
 * IOExceptions.  The SAX ContentHandler interface, however, only permits 
 * emitting SAXExceptions.  While SAXException can wrap an IOException 
 * already, this class allows a wrapped IOException to be identified 
 * by type (for use, e.g., in catch clauses).  
 */
public class IOinSAXException extends SAXException {

    /**
     * wrap an IO exception
     * @param msg     a custom message
     * @param ioex    the original IOException being wrapped
     */
    public IOinSAXException(String msg, IOException ioex) {
        super(msg, ioex);
    }

    /**
     * wrap an IO exception
     * @param msg     a custom message
     * @param ioex    the original IOException being wrapped
     */
    public IOinSAXException(IOException ioex) {
        super("IOException during XML processing: "+ioex.getMessage(), ioex);
    }

    /**
     * wrap an IO exception
     * @param msg     a custom message
     */
    public IOinSAXException(String msg) {
        super(msg);
    }

    /**
     * return the wrapped IOException
     */
    public IOException getIOException() { return (IOException) getException(); }

}