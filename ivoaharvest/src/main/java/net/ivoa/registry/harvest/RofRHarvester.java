package net.ivoa.registry.harvest;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;

import java.io.IOException;

/**
 * a Harvester specialized to extract the publishing registry records from the
 * the Registry of Registries (RofR).  
 * <p>
 * This Harvester will extract its records according to the recommendation 
 * spelled out in the 
 * <a href="http://www.ivoa.net/Documents/Notes/RegistryOfRegistries/RegistryOfRegistries-20070628.html">IVOA Note "The Registry of Registries" v1.00</a>.
 * In particular, it sets the OAI-PMH filtering set to "ivo_publishers".  
 */
public class RofRHarvester extends Harvester {

    /**
     * create a harvester ready to harvest from the RofR
     */
    public RofRHarvester() {
        this(null, null);
    }

    /**
     * create a harvester ready to harvest from the RofR
     * @param endpoint   the base URL to the OAI-PMH interface of the RofR.  If 
     *                     null, the base URL will be retrieved from the 
     *                     properties.  
     */
    public RofRHarvester(URL rofrEndpoint) {
        this(rofrEndpoint, null);
    }

    /**
     * create a harvester ready to harvest from the RofR
     * @param ristd      the Properties that configure how the harvesting is 
     *                     done (see 
     *              {@link net.ivoa.registry.harvest.RIProperties RIProperties}
     *                   for a list of the properties expected as well as 
     *                   this class's class documentation for more information).
     */
    public RofRHarvester(Properties ristd) {
        this(null, ristd);
    }

    /**
     * create a harvester ready to harvest from the RofR
     * @param endpoint   the base URL to the OAI-PMH interface of the RofR.  If 
     *                     null, the base URL will be retrieved from the 
     *                     properties.  
     * @param ristd      the Properties that configure how the harvesting is 
     *                     done (see 
     *              {@link net.ivoa.registry.harvest.RIProperties RIProperties}
     *                   for a list of the properties expected as well as 
     *                   this class's class documentation for more information).
     */
    public RofRHarvester(URL rofrEndpoint, Properties ristd) {
        super(rofrEndpoint(rofrEndpoint, ristd), false, true, ristd);
        String pubset = std.getProperty(PUBLISHERS_OAISET);
        if (pubset == null)
            throw new IllegalArgumentException(PUBLISHERS_OAISET + 
                                               " property not set");
        addSetName(pubset);
    }

    static URL rofrEndpoint(URL rofrEndpoint, Properties ristd) {
        if (rofrEndpoint != null) return rofrEndpoint;

        if (ristd == null) ristd = getStdDefs();
        String ep = ristd.getProperty(ROFR_OAI_ENDPOINT);
        if (ep == null) 
            throw new IllegalArgumentException(ROFR_OAI_ENDPOINT + 
                                               " property not defined");
        try {
            return new URL(ep);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ROFR_OAI_ENDPOINT + 
                                               " URL property is malformed");
        }
    }

}