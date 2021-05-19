package net.ivoa.registry.harvest.iterator;

import net.ivoa.registry.std.RIStandard; 
import net.ivoa.registry.harvest.HarvestingException;
import net.ivoa.registry.harvest.HarvestListener;

import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;
import java.util.Properties;
import java.util.LinkedList;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

/**
 * a document iterator that will traverse across multiple source streams
 */
public abstract class MultisourceDocumentIterator extends DocumentIteratorBase {
    protected VOResourceExtractor source = null;
    Properties harvestInfo = null;
    Properties std = RIStandard.getDefaultDefinitions();
    IOException except = null;
    int page = 0;
    LinkedList<HarvestListener> listeners = new LinkedList<HarvestListener>();

    /**
     * initialize this iterator with the first source stream
     */
    protected MultisourceDocumentIterator(InputStream firstSource) {
        source = new VOResourceExtractor(firstSource, std, ++page);
    }

    /**
     * initialize this iterator with the first source stream
     */
    protected MultisourceDocumentIterator(InputStream firstSource, 
                                          Properties harvestProps) 
    {
        harvestInfo = harvestProps;
        source = new VOResourceExtractor(firstSource, harvestInfo, std, ++page);
    }

    /**
     * a method that returns the next XML source available.
     */
    protected abstract InputStream nextSource() throws IOException;

    /**
     * add a HarvestListener to receive select OAI-PMH data from the stream.
     */
    protected void addListener(HarvestListener listener) {
        listeners.add(listener);
        source.addListener(listener);
    }

    public Reader nextReader() throws HarvestingException, IOException {
        Reader nxt = source.nextReader();
        if (nxt == null) {
            InputStream nxtsource = nextSource();
            if (nxtsource == null) return null;
            source = new VOResourceExtractor(nxtsource,harvestInfo, std, ++page);
            source.addListeners(listeners);  // transfer the listeners
            return this.nextReader();
        }
        return nxt;
    }
    

}
