package net.ivoa.registry.harvest.iterator;

import net.ivoa.registry.harvest.HarvestingException;

import java.io.Reader;
import java.io.IOException;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.parsers.ParserConfigurationException; 

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

/**
 * a base class for implementing a {@link DocumentIterator}.  It provides a 
 * default implementation of {@link #nextDocument()} that wraps around 
 * {@link #nextReader()}.  
 */
public abstract class DocumentIteratorBase implements DocumentIterator {
    DocumentBuilder db = null;

    /**
     * create iterator
     */
    public DocumentIteratorBase() { this(null); }

    /**
     * create iterator using a given DocumentBuilder to parse the 
     * documents.  The builder is only used when 
     * {@link #nextDocument()} is called.  
     */
    public DocumentIteratorBase(DocumentBuilder builder) { db = builder; }

    /**
     * return the next document in the set as a Reader object.
     * @exception IOException   if an error occurs while creating Reader to data
     */
    public abstract Reader nextReader() throws HarvestingException, IOException;
        

    /**
     * return the next document in the set as a parsed XML document
     * @exception IOException   if an error occurs while creating Reader to data
     * @exception SAXException  if an error occurs while parsing the data
     * @exception DOMException  if an error occurs while loading data into a DOM
     * @exception ParserConfigurationException  if an error occurs while 
     *                          creating the XML DOM document.
     */
    public Document nextDocument() 
        throws HarvestingException, IOException, SAXException, DOMException,
               ParserConfigurationException 
    {
        Reader nxt = nextReader();
        if (nxt == null) return null;

        if (db == null) {
            DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
            db = df.newDocumentBuilder();
        }
        Document out = db.parse(new InputSource(nxt));
        nxt.close();
        return out;
    }

}
