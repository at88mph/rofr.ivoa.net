package ncsa.xml.validation;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 

/**
 * a class of static functions for managing validation using the JAXP interface.
 * <p>
 * One of the motivations for this class is the fact that different techniques 
 * are required to do validation between Java 1.4 and Java 1.5.  
 * <p>
 * Note that Xerces JAXP implementation for Java 1.5 (and maybe 1.6) is broken
 * in that validation will only work if they are loaded in a particular order:
 * when schemas include other schemas, the inner most included schemas must 
 * loaded first.  This requirement is passed onto the SchemaLocation object.  
 */
public class ValidationUtils {

    private static Boolean java14 = null;

    final static String JAXP_SCHEMA_LANGUAGE = 
        "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    final static String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    final static String JAXP_SCHEMA_SOURCE = 
        "http://java.sun.com/xml/jaxp/properties/schemaSource";
    final static String SCHEMA_VALIDATION_FEATURE_ID = 
        "http://apache.org/xml/features/validation/schema";
    final static String SCHEMA_FULL_CHECKING_FEATURE_ID = 
        "http://apache.org/xml/features/validation/schema-full-checking";
    final static String EXTERNAL_SCHEMA_LOCATION = 
        "http://apache.org/xml/properties/schema/external-schemaLocation";
    final static String NAMESPACES_FEATURE_ID = 
        "http://xml.org/sax/features/namespaces";
    final static String VALIDATION_FEATURE_ID = 
        "http://xml.org/sax/features/validation";

    /**
     * turn on XML Schema validation for the given factory.  The SchemaLocation
     * object can be provided to register the location of local XML Schema 
     * (xsd) documents.  If none is provided, validation will rely on 
     * the <code>xsi:schemaLocation</code> attribute in the XML documents 
     * being validated.  
     * @param fact   the document builder factory
     * @param sl     the lookup table of XML Schema documents.  If null, none
     *                  will be registered via this method.
     */
    public static void setForXMLValidation(DocumentBuilderFactory fact,
                                           SchemaLocation sl) 
    {
        fact.setNamespaceAware(true);
        fact.setValidating(true);
//         if (false) {
        if (usingJava14()) {
            // make sure you have the fixed Xerces to go along with this!
            // This currently does not work!
            try {
              if (org.apache.xerces.impl.Version.getVersion().indexOf("2.7.1") 
                  < 0)
                  System.err.println("Warning: Xerces-specific validation " +
                                     "may not work with your Xerces version (" +
                                   org.apache.xerces.impl.Version.getVersion() +
                                     "); try v2.7.1");
            } catch (NoClassDefFoundError ex) {  }

            setForSchemaCacheXerces(fact, sl);
        }
        else {
            setForSchemaCacheJAXP(fact, sl);
        }
    }

    /**
     * return true if we are using a Java 1.4 VM
     */
    public static boolean usingJava14() {
        if (java14 == null) {
            String version = System.getProperty("java.version");
            int minor = 5;
            if (version.startsWith("1.")) {
                int dot = version.indexOf(".", 2);
                if (dot > 0) {
                    try {
                        minor = Integer.parseInt(version.substring(2,dot));
                    } catch (NumberFormatException ex) { }
                }
            }

            java14 = (minor < 5) ? Boolean.TRUE : Boolean.FALSE;
        }

        return java14.booleanValue();
    }

    /**
     * set up local cache of schemas for JAXP
     */
    private static void setForSchemaCacheJAXP(DocumentBuilderFactory df,
                                              SchemaLocation sl) 
    {
        try {

            // Xerce's JAXP implementation is broken: it can only validate
            // properly if the schemas are in proper order.  Doh!
            Object[] schemas = null;
            if (sl != null) schemas = sl.getSchemaList();

            // JAXP style validation
            df.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            if (schemas != null) 
                df.setAttribute(JAXP_SCHEMA_SOURCE, schemas);

        }
        catch (IllegalArgumentException ex) {
            throw new 
              InternalError("Configuration error: TrAX features not supported" +
                            " (are you using Java 1.5 or later?)");
        }
    }

    private static void setForSchemaCacheXerces(DocumentBuilderFactory df,
                                                SchemaLocation sl) 
    {
        try {
            df.setAttribute(NAMESPACES_FEATURE_ID, Boolean.TRUE);
            df.setAttribute(VALIDATION_FEATURE_ID, Boolean.TRUE);
            df.setAttribute(SCHEMA_VALIDATION_FEATURE_ID, Boolean.TRUE);
            df.setAttribute(SCHEMA_FULL_CHECKING_FEATURE_ID, Boolean.TRUE);
            if (sl != null)
                df.setAttribute(EXTERNAL_SCHEMA_LOCATION, 
                                sl.getSchemaLocation());
        }
        catch (IllegalArgumentException ex) {
          throw new 
            InternalError("Configuration error: Xerces features not supported" +
                          " (you probably need Java 1.5 or later)");
        }
    }


}

