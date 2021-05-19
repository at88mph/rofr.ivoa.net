package net.ivoa.registry.vores;

/**
 * An enumeration of the legal values of that status VOResrouce attribute.
 * These values indicate the current operational state of the resource it 
 * describes.  
 */
public enum VORStatus {
    /** resource is current available */
    ACTIVE("active"),
    /** resource is current not available */
    INACTIVE("inactive"),
    /** resource has been removed */
    DELETED("deleted");

    private final String v;
    private VORStatus(String s) { v = s; }

    /**
     * return true if a given value is a legal value representing this 
     * status.  
     */
    public boolean equalsValue(String value) { 
        return toString().equals(value); 
    }

    /**
     * return the legal string value for this status that can appear as a 
     * value of the VOResource status attribute.  
     */
    public String toString() { return v; }
}

