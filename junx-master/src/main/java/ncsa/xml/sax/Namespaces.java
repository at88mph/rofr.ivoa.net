package ncsa.xml.sax;

import java.util.Enumeration;

/**
 * An interface for keeping track of what namespaces are in use.
 */
public interface Namespaces {

    /**
     * return the URI currently associated with the given prefix.
     */
    public abstract String getURI(String prefix);

    /**
     * return the preferred prefix for a given namespace.  
     */
    public abstract String getPrefix(String s);

    /**
     * return an enumeration of the currently defined prefixes
     */
    public abstract Enumeration prefixes();

    /**
     * return an enumeration of the currently defined namespaces
     */
    public abstract Enumeration uris();

    /**
     * return the current default namespace
     */
    public abstract String getDefaultNS();

    /**
     * return the location for a given namespace or null if it is not known
     */
    public abstract String getLocation(String namespace);

    /**
     * return an enumeration of the namespaces for which a location is known
     */
    public abstract Enumeration locatedNamespaces();

    /**
     * create an instance of a Namespaces object that is applicable to an
     * ancestor XML node.  
     * @param steps   the number of steps upward in the hierarchy to the 
     *                   desired ancestor element.  
     */
    public abstract Namespaces ancestor(int steps);
}
