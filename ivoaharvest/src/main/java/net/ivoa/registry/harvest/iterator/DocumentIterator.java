package net.ivoa.registry.harvest.iterator;

import net.ivoa.registry.harvest.HarvestingException;

import java.io.Reader;
import java.io.IOException;
import java.util.NoSuchElementException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

/**
 * an interface for iterating through a set of XML documents in a collection.  
 * In the context of a harvester, it will iterate through a set of VOResource
 * records.  
 * <p>
 * Normally, one would iterate through the documents calling exclusively either 
 * {@link #nextReader()} or {@link #nextDocument()}; however, it is possible to 
 * switch back and forth.  Note however, that this will not cause a record to 
 * be returned more than once.  
 * <p>
 * This interface allows for a streaming model the incoming data.  Thus, one
 * cannot predict if more data is available until all of the data is read.  
 * Thus, one stops iterating when either {@link #nextReader()} or 
 * {@link #nextDocument()} return null.
 */
public interface DocumentIterator {

    /**
     * return the next document in the set as a Reader object.  Null is 
     * returned when no more documents are available.  The caller is 
     * responsible for closing the reader when reading is complete.
     * @exception IOException   if an error occurs while creating Reader to data
     */
    public Reader nextReader() throws HarvestingException, IOException;

    /**
     * return the next document in the set as a parsed XML document.  Null is 
     * returned when no more documents are available.  
     * <p>
     * This may be implemented using the {@link #nextReader()} function; if so, 
     * the implementation of nextDocument() should close the Reader 
     * after parsing its document.
     * @exception IOException   if an error occurs while creating Reader to data
     * @exception SAXException  if an error occurs while parsing the data
     * @exception DOMException  if an error occurs while loading data into a DOM
     */
    public Document nextDocument() 
        throws HarvestingException, IOException, SAXException, DOMException,
               ParserConfigurationException;

}
