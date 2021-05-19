package net.ivoa.registry.harvest.iterator;

import net.ivoa.registry.harvest.Harvester;
import net.ivoa.registry.harvest.HarvestedRecord;
import net.ivoa.registry.harvest.HarvestConsumer;
import net.ivoa.registry.harvest.HarvestingException;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;
import java.io.IOException;
import java.io.Reader;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * stream available VOResource records from a remote registry.  Through
 * the {@link net.ivoa.registry.harvest.Harvester Harvester} class this 
 * stitch multiple harvest queries to a registry into a continuous 
 * iterable sequence of records (via the 
 * {@link net.ivoa.registry.harvest.iterator.DocumentIterator DocumentIterator}
 * interface).  
 */
public class HarvestRecordServer implements RecordServer {

    protected Logger logr = null;

    URL ep = null;
    Properties std = null;
    boolean getlocal = true;

    static int workerCount = 0;

    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.  Only locally published records
     * will be returned.  
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     */
    public HarvestRecordServer(URL endpoint) {
        this(endpoint, true);
    }
    
    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.  Only locally published records
     * will be returned.  
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     */
    public HarvestRecordServer(URL endpoint, Logger logger) {
        this(endpoint, true, logger);
    }
    
    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     */
    public HarvestRecordServer(URL endpoint, boolean localOnly, Logger logger) {
        this(endpoint, localOnly, null, logger);
    }

    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     */
    public HarvestRecordServer(URL endpoint, boolean localOnly,
                               Properties ristd, Logger logger) 
    {
        if (logger == null) logger = Logger.getLogger(getClass().getName());
        logr = logger;

        ep = endpoint;
        getlocal = localOnly;
        std = ristd;
    }

    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     */
    public HarvestRecordServer(URL endpoint, boolean localOnly) {
        this(endpoint, localOnly, null);
    }

    /**
     * return an iterator for stepping through the available VOResource
     * records.
     */
    public DocumentIterator records() throws HarvestingException, IOException {

        String hlog = logr.getName() + ".worker" + 
                                           Integer.toString(++workerCount);
        Harvester worker = new Harvester(ep, getlocal, false, std, 
                                         Logger.getLogger(hlog));
        return new HarvestIter(worker);
    }

}

class HarvestIter extends DocumentIteratorBase {

    Harvester harvstr = null;
    Reader nextReader = null;
    Thread worker = null;
    Exception failure = null;

    public HarvestIter(Harvester harvester) {
        harvstr = harvester;
        worker = new Thread(new Runner());
        worker.start();
    }

    public Reader nextReader() throws HarvestingException, IOException {

        while (nextReader == null && failure == null) {
            if (! worker.isAlive()) return null;
            Thread.yield();
        }

        if (failure != null) {
            if (failure instanceof HarvestingException) 
                throw ((HarvestingException) failure);
            else if (failure instanceof IOException) 
                throw ((IOException) failure);
            else if (failure instanceof RuntimeException) 
                throw ((RuntimeException) failure);
            else 
                throw new IllegalStateException("Programmer Error: caught " +
                                                "unexpected exception: " + 
                                                failure.getClass().getName(), 
                                                failure);
        }

        Reader out = nextReader;
        nextReader = null;
        return out;
    }

    class Runner implements Runnable {
        Runner() { }
        public void run() {
            try {
                harvstr.harvest(new Consumer());
            }
            catch (Exception ex) {
                failure = ex;
            }
        }
    }

    class Consumer implements HarvestConsumer {
        @Override public void harvestStarting() { }
        @Override public void harvestDone(boolean success) { }
        @Override public boolean canRestart() { return true; }

        @Override
        public void consume(HarvestedRecord record) throws IOException {
            if (! record.isAvailable()) return;

            while (nextReader != null) 
                Thread.yield();    // actually, should never happen

            nextReader = record.getContentReader();
        }
    }
}

