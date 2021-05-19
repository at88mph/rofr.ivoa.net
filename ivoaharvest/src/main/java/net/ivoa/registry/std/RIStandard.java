package net.ivoa.registry.std;

import java.util.Properties;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.PropertyResourceBundle;
import java.util.Locale;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import javax.xml.namespace.QName;

/**
 * a static utility class for loading properties of the RI standard into 
 * Properties object
 */
public class RIStandard implements RIProperties {

    public final static String defaultVersion = "1.0";

    public final static String PROPERTIES_BASE_NAME = 
        "registryInterfaceDefinitions";

    /**
     * load the properties associated with the requested version of the 
     * standard.  An example Version string is "1.0";
     */
    public static void loadProperties(Properties props, String version) 
        throws IOException
    {
//         ResourceBundle res = 
//             ResourceBundle.getBundle(PROPERTIES_BASE_NAME, 
//                                      new Locale(version));

        String resname = PROPERTIES_BASE_NAME + "_" + version + ".properties";
        InputStream ris = (RIStandard.class).getResourceAsStream(resname);
        if (ris == null) 
            throw new FileNotFoundException("properties for version " + version
                                            + " not found");
        ResourceBundle res = new PropertyResourceBundle(ris);

        String key = null;
        for(Enumeration e = res.getKeys(); e.hasMoreElements();) {
            key = (String) e.nextElement();
            props.setProperty(key, res.getString(key));
        }
    }

    /**
     * return a set of standard definition for a given version of the Registry
     * Interface standard.
     */
    public static Properties getDefinitionsFor(String version) {
        Properties out = new Properties();
        try {
            loadProperties(out, version);
        } catch (IOException ex) {
            // shouldn't happen
            throw new InternalError("config error: no definitions found for " +
                                    "default RI version");
        }
        return out;
    }

    /**
     * return the set of standard definition for a given version of the Registry
     * Interface standard.
     */
    public static Properties getDefaultDefinitions() {
        return getDefinitionsFor(defaultVersion);
    }

    /**
     * return the QName for the standard VOResource root element returned 
     * by Registry interfaces.  
     * @param std   a RIProperties Properties instance that contains the 
     *                RI standard strings.  That is, a Properties instance
     *                returned by {@link #getDefaultDefinitions()} or 
     *                {@link #getDefintionsFor(String)}.  If null, the 
     *                Properties returned by {@link #getDefaultDefinitions()} 
     *                will be used.  
     */
    public static QName getRIResourceRoot(Properties std) {
        if (std == null) std = getDefaultDefinitions();
        String uri = std.getProperty(REGISTRY_INTERFACE_NAMESPACE);
        String name = std.getProperty(RESOURCE_ELEMENT);
        if (uri == null)
            throw new IllegalStateException("REGISTRY_INTERFACE_NAMESPACE not defined in standard properties");
        if (name == null)
            throw new IllegalStateException("REGISTRY_ELEMENT not defined in standard properties");
        return new QName(uri, name);
    }

    /**
     * return the QName for the standard VOResource root element returned 
     * by Registry interfaces (assuming the default standard).
     */
    public static QName getRIResourceRoot() { return getRIResourceRoot(null); }

    public static void main(String[] args) {
        String version = "1.0";
        if (args.length > 0) version = args[0];

        Properties p = new Properties();
        try {
            RIStandard.loadProperties(p, version);

            p.store(System.out, "Properties for RI version " + version);
        }
        catch (IOException ex) {
            throw new RuntimeException("Trouble printing with Properties." + 
                                       "store(): " + ex.getMessage());
        }
    }
}
