package ncsa.xml.extractor;

import ncsa.xml.sax.Namespaces;
import org.xml.sax.ContentHandler;

/**
 * a special SAX content handling interface that provides additional control
 * for controlling the extraction of nodes via the ExtractingParser.
 */
public interface ExtractingContentHandler extends ContentHandler {

    /**
     * provide an ExportController that should be used for exporting Nodes
     */
    public void setExportController(ExportController exportcontroller);

    /**
     * provide the Namespaces object that should be used for tracking namespace
     * definitions.  
     */
    public void setNamespaces(Namespaces namespaces);
}
