package net.ivoa.registry.util;

import java.util.Date;

/**
 * a container class that contains key information about a resource
 */
public class ResourceSummary {

    String title = null;
    String shortName = null;
    String id = null;
    String status = null;
    String harvestedFromID = null;
    String harvestedFromEP = null;
    Date harvested = null;
    Date updated = null;

    /**
     * This class is intended to be instantiated by a ResourcePeeker class
     */
    protected ResourceSummary() { }

    /**
     * return the title of the resource
     */
    public final String getTitle() { return title; }

    /**
     * return the short name of the resource 
     */
    public final String getShortName() { return shortName; }

    /** 
     * return the IVOA identifier for the resource
     */
    public final String getID() { return id; }

    /** 
     * return the status of the resource
     */
    public final String getStatus() { return status; }

    /**
     * return the source of the harvested record
     */
    public final String getHarvestedFromID() { return harvestedFromID; }

    /**
     * return the source of the harvested record
     */
    public final String getHarvestedFromEndPoint() { return harvestedFromEP; }

    /**
     * return the date when the local record was retrieved
     */
    public Date getHarvestedDate() { return harvested; }

    /**
     * return the date when the record was remotely updated. 
     */
    public Date getUpdatedDate() { return updated; }

    public int hashCode() { return getID().hashCode(); }
}