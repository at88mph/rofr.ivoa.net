package net.ivoa.registry.harvest;

import net.ivoa.registry.util.ResourceSummary;

import java.util.Date;

/**
 * a container class that contains key information about a publishing registry 
 */
public class PublishingRegistry extends ResourceSummary {

    String oaiep = null;

    PublishingRegistry() { super(); }

    /**
     * return the OAI-PMH harvesting endpoint URL for the registry.
     */
    public String getHarvestingEndpoint() { return oaiep; } 

}