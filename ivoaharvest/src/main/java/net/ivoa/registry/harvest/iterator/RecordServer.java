package net.ivoa.registry.harvest.iterator;

import net.ivoa.registry.harvest.HarvestingException;

import java.io.Reader;
import java.io.IOException;

/**
 * an interface for accessing a set of VOResource records as XML documents.
 */
public interface RecordServer {

    /**
     * return an iterator for stepping through the available VOResource
     * records.
     */
    public DocumentIterator records() throws HarvestingException, IOException;

}