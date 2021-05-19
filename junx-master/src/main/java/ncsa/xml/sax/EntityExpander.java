/*
 * NCSA BIMAgrid
 * Radio Astronomy Imaging Group
 * National Center for Supercomputing Applications
 * University of Illinois at Urbana-Champaign
 * 605 E. Springfield, Champaign IL 61820
 * rai@ncsa.uiuc.edu
 *-------------------------------------------------------------------------
 * History: 
 *  2003/08/08   rlp  initial version
 */
package ncsa.xml.sax;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * a class that can lookup XML entities and expand them to their ASCII form.
 * <p>
 * An XML entity has the form "&tag;" which typically represents text that 
 * is difficult to represent in some contexts, particularly raw XML.  In 
 * typical use of this class, one passes text as Strings to the 
 * {@link #expand(String) expand()} method, which returns a revision of the 
 * text with the entity occurrances replaced with the text they represent.
 * </p>
 * <p>
 * At construction time, this class comes with a few entities (those specified
 * by the XML 1.0 standard) already defined:
 * <table border="1" cellspacing="1" cellpadding="1" width="25%">
 * <thead><tr><th>Entity</th><th>Value</th></tr></thead><tbody>
 *   <tr><td>&amp;amp;</td><td align="center">&amp;</td>
 *   <tr><td>&amp;lt;</td><td align="center">&lt;</td>
 *   <tr><td>&amp;gt;</td><td align="center">&gt;</td>
 *   <tr><td>&amp;apos;</td><td align="center">&apos;</td>
 *   <tr><td>&amp;quot;</td><td align="center">&quot;</td>
 * </tbody></table> </p>
 * 
 * <p>
 * Additional entities can be added to this class via 
 * {@link #define(String, String) define()}.  
 * </p>
 */
public class EntityExpander {

    private Properties map = new Properties();
    private boolean allowRedefine = false;

    public EntityExpander() {
        define("amp", "&");
        define("lt", "<");
        define("gt", ">");
        define("quot", "\"");
        define("apos", "'");
    }

    /**
     * define an entity
     * @param entname    the name of the entity (without the & or ;)
     * @param value      the value to expand the entity to
     * @return boolean   false if input redefines an entity and redefinitions
     *                     are disabled.
     */
    public synchronized boolean define(String entname, String value) {
        entname = "&" + entname + ";";
        if (! allowRedefine && map.get(entname) != null) return false;
        map.put(entname, value);
        return true;
    }

    /**
     * return the value that an entity is defined to expand to
     * @param entname    the name of the entity (without the & or ;)
     * @return String    the value to expand the entity to or null if 
     *                      the entity is not defined.
     */
    public String getExpansion(String entname) {
        return map.getProperty("&"+entname+';');
    }

    /**
     * set whether entities can be redefined once loaded.  
     */
    public void allowRedefine(boolean tf) {
        allowRedefine = tf;
    }

    /**
     * return whether entities can be redefined once loaded.  
     */
    public boolean redefineAllowed() {
        return allowRedefine;
    }

    /**
     * search for and expand any recognized entities
     */
    public synchronized String expand(String in) {
        int a, p = 0, n = 0;
        Enumeration e;
        boolean found = false;
        String key = null, sub = null;
        while ((a = in.indexOf("&", p)) >= 0) {
            for(found=false, e = map.keys(); e.hasMoreElements();) {
                key = (String) e.nextElement();
                if (in.startsWith(key, a)) {
                    found = true;
                    sub = map.getProperty(key);
                    in = in.substring(0, a) + sub + 
                        in.substring(a + key.length());
                    p = a + sub.length();
                    break;
                }
            } 
            if(! found) p++;
        } 

        return in;
    }

    public static void main(String args[]) {
        EntityExpander exp = new EntityExpander();
        exp.define("apos", "`");
        exp.define("tab", "\t");
        for(int i = 0; i < args.length; i++)
            System.out.println(exp.expand(args[i]));
    }
}
