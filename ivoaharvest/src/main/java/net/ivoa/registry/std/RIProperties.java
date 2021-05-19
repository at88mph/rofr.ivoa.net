package net.ivoa.registry.std;

/**
 * an interface defining the names of properties that describe the 
 * Registry Interface standard.  In general, the values are identical to the 
 * field names.  
 */
public interface RIProperties {

    /**
     * the property name for the Registry Interface version
     */
    public final static String RI_VERSION = "1.0";

    /**
     * the property name for the namespace URI for the OAI-PMH response format
     */
    public final static String OAI_NAMESPACE = "OAI_NAMESPACE";

    /**
     * the property name for the namespace URI for the OAI Dublin core 
     * metadata format namespace.
     */
    public final static String OAI_DC_NAMESPACE = "OAI_DC_NAMESPACE";

    /**
     * the property name for the namespace URI used by the Registry 
     * Interface input/output messages
     */
    public final static String REGISTRY_INTERFACE_NAMESPACE = 
        "REGISTRY_INTERFACE_NAMESPACE";

    /**
     * the property name for the root element of a VOResource record
     */
    public final static String RESOURCE_ELEMENT = "RESOURCE_ELEMENT";

    /**
     * the property name for the OAI set that includes only those records
     * that have been originally published with a registry.
     */
    public final static String MANAGED_OAISET = "MANAGED_OAISET";

    /**
     * the property name for the OAI set that includes only those Registry
     * records that describe publishing registries.  This is used when 
     * harvesting from the Registy of Registries.
     */
    public final static String PUBLISHERS_OAISET = "PUBLISHERS_OAISET";

    /**
     * the property name for the OAI metadata format prefix reprensenting 
     * VOResource v1.0.
     */
    public final static String VORESOURCE_OAIFORMAT = "VORESOURCE_OAIFORMAT";

    /**
     * the property name for the endpoint URL of the Registry of Registries'
     * OAI-PMH service.
     */
    public final static String ROFR_OAI_ENDPOINT = "ROFR_OAI_ENDPOINT";

}
