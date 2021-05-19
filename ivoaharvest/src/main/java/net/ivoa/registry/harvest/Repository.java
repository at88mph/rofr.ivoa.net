package net.ivoa.registry.harvest;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * a class that manages persisted harvested records from a registry.  In 
 * particular, records can be harvested into a particular storage scheme
 * and updated.  
 */
public abstract class Repository {

    protected String endpointURL = null;
    protected boolean ownedOnly = true;
    protected Logger logr = null;
    String regid = null;
    String name = null;
    HashSet<HarvestListener> listeners = new HashSet<HarvestListener>();

    /**
     * create repository that will harvest its records from a given endpoint
     * @param harvestEndpoint  the harvesting service endpoint
     */
    public Repository(String harvestEndpoint) {
        this(harvestEndpoint, null);
    }

    /**
     * create repository that will harvest its records from a given endpoint
     * @param harvestEndpoint  the harvesting service endpoint
     * @param logger           the Logger to use in this instance.  If null,
     *                           a default will be used.
     */
    public Repository(String harvestEndpoint, Logger logger) {
        logr = logger;
        if (logr == null) logr = Logger.getLogger(getClass().getName());

        endpointURL = harvestEndpoint;
    }

    /**
     * create repository that will harvest its records from a given endpoint
     * @param harvestEndpoint  the harvesting service endpoint
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     * @param logger           the Logger to use in this instance.  If null,
     *                           a default will be used.
     */
    public Repository(String harvestEndpoint, boolean localOnly, Logger logger) {
        this(harvestEndpoint, logger);
        ownedOnly = localOnly;
    }

    /**
     * set the ID of the registry we are harvesting from.  This will be 
     * ignored unless this class was instantiated with embedHarvestInfo=true.
     */
    public void setRegistryID(String id) { regid = id; }

    /**
     * set the ID of the registry we are harvesting from.  This will be 
     * ignored unless this class was instantiated with embedHarvestInfo=true.
     */
    public String getRegistryID() {  return regid;  }

    /**
     * set the name of the registry we are harvesting from.  
     */
    public void setRegistryName(String name) { this.name = name; }

    /**
     * get the name of the registry we are harvesting from.  
     */
    public String getRegistryName() {  return name;  }

    /**
     * accept a harvest listener to be attached to each harvesting process
     */
    public void addHarvestListener(HarvestListener listener) {
        if (listeners == null) listeners = new HashSet<HarvestListener>();
        listeners.add(listener);
    }

    /**
     * remove a harvest listener to attached to each harvesting process
     */
    public void removeHarvestListener(HarvestListener listener) {
        if (listeners == null) return;
        listeners.remove(listener);
    }

    /**
     * create the harvester that should be used to update the resource 
     * record content of this repository.  
     * @param incremental  if true, prepare a harvester that will only update 
     *                        the records that have changed since the last 
     *                        harvest.  This implementation ignores this 
     *                        parameter. 
     * @param harvestURL   the URL to harvest from.  This may be a URL
     *                        modified from the one passed to this repository 
     *                        at construction time.
     */
    protected Harvester createHarvester(boolean incremental, URL harvestURL)
        throws IOException
    {
        Logger huse = Logger.getLogger(logr.getName()+".harvester");
        huse.setLevel(null);
        Harvester out = new Harvester(harvestURL, ownedOnly, true, huse);
        if (regid != null) out.setRegistryID(regid);
        for(HarvestListener listener : listeners) 
            out.addHarvestListener(listener);
        return out;
    }

    /**
     * create the consumer that should be used to persist the harvested records.
     * @param incremental  if true, prepare a harvester that will only update 
     *                        the records that have changed since the last 
     *                        harvest.  
     */
    protected abstract HarvestConsumer createConsumer(boolean incremental)
        throws IOException;

    /**
     * return the OAI-formatted string encodeing the time of the last 
     * harvest that updated this repository.  This is should be the 
     * OAI response timestamp from the first page of harvesting results 
     * received.  
     */
    public abstract String getLastHarvest() throws IOException;

    /**
     * update the resource record content of this repository via standard
     * harvesting mechanisms.  This method should call 
     * {@link #createHarvester(boolean) createHarvester()}
     * @param incremental   if true, attempt to get only those records 
     *                          updated since the last harvest.  
     * @returns int   the number of new records harvested.  If incremental is 
     *                   false, this number should be equal to the total 
     *                   number available.  
     */
    public abstract int update(boolean incremental) 
        throws HarvestingException, IOException;

}
