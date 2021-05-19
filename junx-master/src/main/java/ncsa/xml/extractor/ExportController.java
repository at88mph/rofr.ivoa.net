package ncsa.xml.extractor;

/**
 * an interface for exporting nodes as part of an ExtractingParser.  An object
 * of this type will be passed to a ExtractingContentHandler when it is set
 * as a content handler for an ExtractingParser.  
 */
public interface ExportController {

    /**
     * signal that the node currently being parsed should be extracted.  
     * The client of the associated Extractor interface (using an 
     * ExtractingParser) will subsequently receive a Reader (via a call to 
     * nextNode()) for reading this node in its entirety.  
     * @exception ExportingNotReadyException  if an ancestor of the current 
     *       node is already marked for extraction, or if exporting nodes of 
     *       typeis not supported.
     */
    public void exportNode() throws ExportingNotReadyException;

    /**
     * return true if this node or one of its ancestor's is currently marked 
     * to be extracted.  
     */
    public boolean isExporting();
}
