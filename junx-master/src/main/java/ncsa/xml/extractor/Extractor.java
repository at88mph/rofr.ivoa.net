package ncsa.xml.extractor;

import java.io.IOException;
import java.io.Reader;

/**
 * an interface for extracting individual nodes from an XML document.  This 
 * allows one to split a large XML document into separate standalone documents 
 * with confidence that all namespace definitions will be carried along as 
 * necessary.  The implementation controls which nodes are available from 
 * the document.  In any case, the Reader's returned by this interface will 
 * be initially positioned at the start of a node and will signal EOF when 
 * at the end of the node.  
 */
public interface Extractor {

    /**
     * return a reader that will read a next available node from the document.
     * The Reader will be positioned at the start of the node, and an EOF will
     * be signalled in the usual way when the end of the node has been read.  
     * Null is returned if there are no more nodes available.  
     */
    public Reader nextNode() throws IOException;

    /**
     * returns true if the Readers will be returning them as if they were 
     * complete documents.  This will usually mean that they will start with 
     * an XML declaration (i.e. "<?xml ...?>").  
     */
    public boolean returnsDoc();
}
