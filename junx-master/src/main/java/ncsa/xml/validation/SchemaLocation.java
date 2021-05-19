package ncsa.xml.validation;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.StringTokenizer;
import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * a container for mappings from XML Schema namespaces to Schema files.
 *
 * <p>
 * This class provides a mechanism for maintaining a local cache of Schema 
 * files that a validating parser can pull a schema from in lieu of 
 * downloading it from the document-specified location.  It can provide 
 * the locations to a parser in two ways:
 * <ul>
 *   <li> the JAXP way: as an array of Objects containing the locations
 *        via <code>getSchemaList()</code> </li>
 *   <li> the Xerces-specific way: as a String in the format of the 
 *        <code>xsi:schemaLocation</code> attribute via the 
 *        <code>getSchemaLocation()</code>. </li>
 * </ul>
 *
 * <blockquote><strong>Note:</strong><br />
 * While the specific procedure for passing the output of one of 
 * these two method is not covered here (see the Xerces and JAXP 
 * documentation), note that <em>various versions of Xerces are in various 
 * states of broken-ness</em>, and these mechanisms do not always work 
 * properly.  For the Xerces-specific way, you need Xerces v2.7 or later 
 * (and possibly Java 1.4).  For the JAXP way, schemas must be loaded in 
 * dependency order (i.e. such that schema's dependency have already been 
 * loaded; see Xerces bug #30481).  This class attempts to address this 
 * latter problem by listing the schema locations in the order that they 
 * were added.  It is up to the user to figure out what the proper order 
 * is.  
 * </blockquote>
 *
 * Schema mappings can either be loaded in individually via the 
 * <code>addLocation()</code> or <code>addLocations()</code> methods, or 
 * more typically via a flat file (with a default name of 
 * <code>schemaLocation.txt</code>) that can be loaded via one of the 
 * <code>load()</code> methods.  The <code>autoload()</code> methods 
 * provide the ability to load the file from the CLASSPATH as a resource.
 *
 * <p>
 * Each line of the file contains a namespace, a space, and a location.  
 * The location can be a URI or a file path.  If the file is loaded with
 * <code>load()</code>, then relative file paths are interpreted as via the 
 * current working directory.  If it was loaded as a resource, then the
 * full location specified by a relative path is determined by searching, 
 * in order, the following places:
 * <ol>
 *    <li> the path relative to the package of a specified class, </li>
 *    <li> the path relative to the SchemaLocation's package (or its 
 *           derivative class' package), </li>
 *    <li> the path relative to the current working directory.  </li>
 * </ol>
 * The first location the schema file is found is taken as the location.  
 * Note that this search is done at the time locations are requested (via 
 * <code>getLocation()</code>) and only for those locations specified with 
 * relative paths.
 *
 * <p>
 * This class will look for a file containing the mappings in various 
 * locations.
 */
public class SchemaLocation implements Cloneable {

    Properties mappings = null;

    // We must preserve the order to get around a JAXP bug
    Vector order = null;

    /**
     * the default name of file to look for namespace-schema location 
     * mappings in.
     */
    public final static String DEFAULT_SL_FILE = "schemaLocation.txt";

    /**
     * create an empty set of mappings
     */
    public SchemaLocation() {
        mappings = new Properties();
        order = new Vector();
    }

    /**
     * create a set of mappings with defaults provided by another mapping
     */
    public SchemaLocation(SchemaLocation defaults) {
        mappings = new Properties(defaults.mappings);
        order = (Vector) order.clone();
    }

    /**
     * create a set of mappings with defaults loaded automatically.  See 
     * autoload(Class) for details.
     */
    public SchemaLocation(Class refclass) {
        this();
        autoload(refclass);
    }

    /**
     * add a namespace-schema location mapping.  
     * @param nsuri     the namespace URI
     * @param location  either a URL or a file path pointing the XML schema
     *                    document defining that namespace.
     */
    public void addLocation(String nsuri, String location) {
        addLocation(nsuri, location, mappings, order);
    }

    static void addLocation(String nsuri, String location, 
                            Properties mappings, Vector order) 
    {
        boolean readd = mappings.containsKey(nsuri);
        mappings.setProperty(nsuri, location);
        if (! readd) order.addElement(nsuri);
    }

    /**
     * add namespace-schema location mappings.  The format of the input string
     * is the same as for the value of an xsi:schemaLocation attribute:  it is
     * a list of namespace identifier and location pairs, 
     * e.g., "http://www.ivoa.net/xml/ADQL/v1.0 localdir/ADQL-v1.0.xsd ..."
     * The locations can be URLs or pathnames on a local filesystem.
     * If there is an odd number of names in the list (i.e. ends a namespace 
     * without a location), the last name is silently ignored.  
     * @param schemaLocations     the namespace-schema location pairs
     */
    public void addLocations(String schemaLocations) {
        addLocations(schemaLocations, mappings, order);
    }

    static void addLocations(String schemaLocations, 
                             Properties map, Vector order) 
    {
        String ns = null, loc = null;
        StringTokenizer tok = new StringTokenizer(schemaLocations);

        while (tok.hasMoreTokens()) {
            ns = tok.nextToken();
            if (! tok.hasMoreTokens()) break;
            loc = tok.nextToken();
            addLocation(ns, loc, map, order);
        }
    }

    /**
     * look for a resource which will contain namespace-schema location 
     * pairs and load those mappings.  Location paths given in the resource 
     * will interpreted as pointing to a schema file stored as a resource
     * unless that path is in the form of a URL.  
     * @param resourceName  a relative path name of the resource to read 
     *                         mappings from.
     * @param refclass      a class whose package will provide the reference 
     *                         point for looking for the resource.  If null,
     *                         the top of the classpath will be taken as the 
     *                         reference point.  
     */
    public void loadFromResource(String resourceName, Class refclass) 
         throws FileNotFoundException
    {
        InputStream mapstrm = (refclass == null) 
            ? getClass().getResourceAsStream(resourceName)
            : refclass.getResourceAsStream(resourceName);

        if (mapstrm == null) 
            throw new FileNotFoundException("Can't find resource: " + 
                                            resourceName);

        try {
            load(new InputStreamReader(mapstrm), true, refclass);
        } catch (IOException ex) { }
    }

    /**
     * load the namespace-schema location pairs from the given input stream
     */
    public void load(Reader in) throws IOException {
        load(in, false, null);
    }

    /**
     * load the namespace-schema location pairs from a named file
     */
    public void load(String file) throws FileNotFoundException, IOException {
        load(new FileReader(file), false, null);
    }

    /**
     * load the namespace-schema location pairs from a named file
     */
    public void load(File file) throws FileNotFoundException, IOException {
        load(new FileReader(file), false, null);
    }

    void load(Reader in, boolean asResource, Class refclass) 
         throws FileNotFoundException, IOException 
    {
        BufferedReader rdr = new BufferedReader(in);
        String line = null, ns = null, word = null;
        StringTokenizer tok;
        while ((line = rdr.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.charAt(0) == '#') continue;

            tok = new StringTokenizer(line);
            while (tok.hasMoreTokens()) {
                word = tok.nextToken();
                if (ns == null) {
                    ns = word;
                }
                else {
                    if (asResource && !word.contains(":")) {
                        String clsname = (refclass == null) ? "" : refclass.getName();
                        word = "classpath:" + clsname + File.separator + word;
                    }
                    addLocation(ns, word);
                    ns = null;
                }
            }
        }
	rdr.close();
    }

    /**
     * return the location of the schema file identified by the given 
     * namespace
     */
    public String getLocation(String namespace) {
        String out = mappings.getProperty(namespace);
        if (out == null) return null;

        if (out.startsWith("classpath:")) {
            int firstslash = out.indexOf('/',10);
            String classname = out.substring(10,firstslash);
            out = out.substring(firstslash+1);
            try {
                URL resurl;
                if (classname.length() > 0) {
                    Class refclass = Class.forName(classname);
                    resurl = refclass.getResource(out);
                }
                else {
                    resurl = getClass().getResource(out);
                }
                out = resurl.toString(); 
            }
            catch (Exception ex) { }
        }
        else if (out.indexOf(':') < 0) {
            File localfile = new File(out);
            if (! localfile.isAbsolute()) out = localfile.getAbsolutePath();
        }

        return out; 
    }

    /**
     * return a schema location list in a form needed by the XML Parser.
     */
    public String getSchemaLocation() {
        StringBuffer out = new StringBuffer();
        for(Enumeration e=mappings.propertyNames(); e.hasMoreElements();) {
            String ns = (String) e.nextElement();
            out.append(ns).append(' ').append(getLocation(ns));
            if (e.hasMoreElements()) out.append(' ');
        }

        return out.toString();
    }

    /** 
     * return the list of schema files as needed by the JAXP API.  This 
     * implementation, to address a bug in Xerces' JAXP implementation,
     * will return the schema locations in the order they were added.  
     */
    public Object[] getSchemaList() {
        Properties use = (Properties) mappings.clone();
        Object[] out = new Object[use.size()];
        int i=0, j=0;
        String ns=null, loc=null;

        for(i=0, j=0; i < order.size() && j < out.length; i++) {
            ns = (String) order.elementAt(i);
            loc = getLocation(ns);
            if (loc == null) continue;
            if (loc.indexOf(':') < 0) 
                out[j] = "file:" + loc;
            else 
                out[j] = loc;
            use.remove(ns);
            j++;
        }

        // catch any leftovers
        for(Enumeration e=use.propertyNames(); e.hasMoreElements(); j++) {
            ns = (String) e.nextElement();
            loc = getLocation(ns);
            if (loc.indexOf(':') < 0) 
                out[j] = "file:" + loc;
            else 
                out[j] = loc;
        }

        return out;
    }

    /**
     * look for schemaLocation files in the classpath and on local disk
     * and load their contents.  This method will look in the following places
     * and in the following order:
     * <ol>
     *   <li> as a resource relative to this SchemaLocation class, </li>
     *   <li> as a resource relative to the given class (if provided), </li>
     *   <li> as a resource relative to the top of the classpath, </li>
     *   <li> as a local file relative to the current working directory. </li>
     * </ol>
     * Duplicate mappings found later in the sequence will override previous 
     * ones.  
     * @param resourceName  a relative path name of the resource to read 
     *                         mappings from.
     * @param refclass      a class whose package will provide the reference 
     *                         point for looking for the resources in the 
     *                         classpath.  This can be null.  
     */
    public void autoload(String resourceName, Class refclass) {

        // look first in the classpath relative to this class
        try {
            loadFromResource(resourceName, getClass());
        } catch (FileNotFoundException ex) { }

        // look next in the classpath relative to the given class.
        if (refclass != null) {
            try {
                loadFromResource(resourceName, refclass);
            } catch (FileNotFoundException ex) { }
        }

        // next look at the top of the classpath
        try {
            loadFromResource(resourceName, null);
        } catch (FileNotFoundException ex) { }

        // now look in the current directory
        File resFile = new File(resourceName);
        if (resFile.exists()) {
            try {
                load(resFile);
            } 
            catch (FileNotFoundException ex) { }
            catch (IOException ex) { 
                System.err.println("warning: failed to read locations from " + 
                                   resFile.getAbsolutePath() + ": " + 
                                   ex.getMessage());
            }
        }
    }

    /**
     * look for schemaLocation files called "schemaLocation.txt" in the 
     * classpath and on local disk and load their contents.  This method 
     * will look in the following places and in the following order:
     * <ol>
     *   <li> as a resource relative to the given class (if provided), </li>
     *   <li> as a resource relative to the top of the classpath, </li>
     *   <li> as a local file relative to the current working directory. </li>
     * </ol>
     * Duplicate mappings found later in the sequence will override previous 
     * ones.  
     * @param refclass      a class whose package will provide the reference 
     *                         point for looking for the resources in the 
     *                         classpath.  This can be null.  
     */
    public void autoload(Class refclass) {
        autoload(DEFAULT_SL_FILE, refclass);
    }

    /**
     * clone this set of mappings
     */
    public Object clone() {
        try {
            SchemaLocation out = (SchemaLocation) super.clone();
            out.mappings = (Properties) mappings.clone();
            out.order = (Vector) order.clone();
            return out;
        } catch (CloneNotSupportedException ex) {
            throw new InternalError("programming error in support of clone");
        }
    }

    public String toString() {
        return getSchemaLocation();
    }
}
