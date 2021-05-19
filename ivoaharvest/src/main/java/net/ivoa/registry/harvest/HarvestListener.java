package net.ivoa.registry.harvest;

import java.util.Set;

/**
 * a listener interface to the OAI-PMH harvesting process.  It can be used 
 * to retrieve selected data from the OAI-PMH ListRecords data.  
 */
public interface HarvestListener {

    /**
     * the item name for the date of response from the registry.
     */
    public final static String RESPONSE_DATE = "responseDate";

    /**
     * the item name for the date of response from the registry.
     * This item is sent whenever a new page of records is requested.
     */
    public final static String REQUEST_URL = "requestURL";

    /**
     * the item name for the name of the registry.
     * This item is sent whenever harvesting is initiated, if the name
     * is known.  This name is application dependent and does not necessarily
     * correspond to the registry's ID, shortName, or title.  
     */
    public final static String REGISTRY_NAME = "registryName";

    /**
     * the item name for the status of the completed harvesting process.
     * This item is sent when harvesting from one registry is finished, and 
     * the value will be the name of the registry.  
     */
    public final static String HARVEST_COMPLETE = "complete";

    /**
     * the item name for the status of an individual harvested record.
     * The value is either "current" or "deleted".
     */
    public final static String RECORD_STATUS = "status";

    /**
     * add the names of the items desired by this listener.  This information
     * is used by the harvester to optimize its observations on the stream.
     *
     * @param names   a Set that the listener should add the names of the 
     *                    desierd items into.  
     */
    public void tellWantedItems(Set<String> names);

    /**
     * receive an item of information from the harvesting stream
     * @param page      the number of the page (starting with 1) that the value 
     *                    appears in.  A page represents a separate ListRecords 
     *                    call, using a resumption token, when applicable.  
     * @param id        the identifier of the record that this piece of 
     *                    information belongs to.  This id will be null if 
     *                    the item value is not part of a record (e.g. it is 
     *                     part of the OAI-PMH preamble).
     * @param itemName  the name of the item of information be provided
     * @parma value     the value of the item.  
     */
    public void harvestInfo(int page, String id, String itemName, String value);

}