package ncsa.xml.saxfilter;

import java.io.Reader;
import java.io.FileReader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.namespace.QName;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;

/**
 * A class for modifying the XML content on-the-fly as it is copied from 
 * a source to a destination.  This be non-concurrently applied to muliple
 * XML source streams.  
 */
public class XMLStreamEditor {

    MultiSFContentHandler handlers = new MultiSFContentHandler();

    /**
     * create an XML stream editor that passes input XML data unchanged
     */
    public XMLStreamEditor() {
    }

    /**
     * create an XML stream editor with a specific content handler to do 
     * modifications
     */
    public XMLStreamEditor(ContentHandler handler) {
        handlers.addHandler(handler);
    }

    /**
     * add a ContentHandler that can edit parts of the resource.  To actually
     * edit the resource, the handler must be a SAXFilterContentHandler.
     * generic ContentHandlers can be added, however, to snoop on the 
     * stream while it is being updated.  The order that handlers are added 
     * to this editor is the order that they are sent SAX events.  
     */
    public void addContentHandler(ContentHandler ch) {
        handlers.addHandler(ch);
    }

    /**
     * provide an iterator to the currently added ContentHandlers
     */
    public Iterator<ContentHandler> handlers() { return handlers.iterator(); }

    /**
     * return a Reader that will serve up the modified XML content
     */
    protected Reader modifiedReader(Reader unmodified) {
        return new SAXFilteredReader(unmodified, handlers);
    }

    /**
     * copy an input XML stream to an output stream, modifying it along the 
     * way.  
     * @param in    the source stream containing the XML data to update.  
     *                  It will be read to its end.
     * @param out   the destination stream to write the updated XML to.  It
     *                  will not be closed at the end of the transfer.
     */
    public void modify(Reader in, Writer out) throws IOException {
        Reader instrm = modifiedReader(in);
        char[] buf = new char[1024];
        int n = 0;
        while ((n = instrm.read(buf)) >= 0)
            out.write(buf, 0, n);
    }

    /**
     * copy the XML content in an input file to an output file, updating the 
     * data along the way.  
     * @param in    the file containing the source XML data to update.  
     * @param out   the file to write the updated XML to.  
     */
    public void modify(File in, File out) throws IOException {
        FileReader instrm = null;
        FileWriter outstrm = null;
        try {
            instrm = new FileReader(in);
            outstrm = new FileWriter(in);
            modify(instrm, outstrm);
        }
        finally {
            if (instrm != null) {
                try { instrm.close(); } catch (IOException ex) { }
            }
            if (outstrm != null) {
                try { outstrm.close(); } catch (IOException ex) { }
            }
        }
    }


}

