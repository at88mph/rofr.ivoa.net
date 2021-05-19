/*
 * Created by Ray Plante for the National Virtual Observatory
 * as part of ivoaregistry (RI1 search library)
 * c. 2006
 * Adapted for ivoaharvester
 */
package net.ivoa.registry.vores;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * a class for extracting VOResource metadata out of a DOM tree.
 */
public class Capability extends Metadata {

    private String captype = null;
    private String standardid = null;

    /**
     * create a Capability metadata extractor
     */
    public Capability(Element el) {
        super(el);
    }

    Capability(Element el, String xsitype, String stdid) {
        super(el);
        if (xsitype != null && xsitype.length() == 0) xsitype = "Capability";
        captype = xsitype;
        standardid = stdid;
    }

    /**
     * return the capability class.  This is the value of the xsi:type attribute
     * on the capability element.  The namespace prefix is stripped off
     * before returning.  If an xsi:type attribute is not specified, the 
     * default string, "Capability" is returned.  
     */
    public String getCapabilityClass() {
        if (captype != null) return captype;

        String out = ((Element) getDOMNode()).getAttributeNS(XSI_NS, "type");
        if (out == null || out.length() == 0) out = "Capability";
        int c = out.indexOf(":");
        if (c >= 0) out = out.substring(c+1);
        captype = out;

        return out;
    }

    /**
     * return the standard identifier for this capability or null if it 
     * does not have one set.
     */
    public String getStandardID() {
        if (standardid != null) return standardid;

        standardid = ((Element) getDOMNode()).getAttribute("standardID");
        return standardid;
    }

    /**
     * return all validation levels attached to this capability.
     * (Resource-level validation levels are not returned.)
     */
    public Map<String, Integer> getValidationLevels() {
        return Metadata.getValidationLevels(this);
    }

    /**
     * return the validation level attached to this capability as assigned by 
     * a specified registry.
     * Null is returned if the specified registry did not set a 
     * legal value.
     * @param who   the IVOA Identifier for the registry that set the validation
     *                 level
     */
    public Integer getValidationLevelBy(String who) {
        return Metadata.getValidationLevelBy(this, who);
    }

    /**
     * return the standard Interface description for this capability
     * @param version  the version of the protocol to get; if null, 1.0 is 
     *                   assumed.
     * @param lenient  if true, be lenient in determining which Interface is 
     *                   most likely refers to the standard one.  In particular,
     *                   if there is only one interface and it is not marked 
     *                   with a version, that interface will be returned. 
     */
    public Metadata getStandardInterface(String version, boolean lenient) {
        if (version == null) version = "1.0";
        return getInterface("std", version, lenient);
    }

    /**
     * return a specified Interface description for this capability.  
     * @param role     the role to look for
     * @param version  the version of the protocol to get; if null, 1.0 is 
     *                   assumed.
     */
    public Metadata getInterface(String role, String version) {
        return getInterface(role, version, false);
    }

    /**
     * return the Interface description for this capability.  Note that 
     * this implementation interprets empty values for the role and version
     * attributes on an Interface node as sematically equivalent to not 
     * specifying them.  
     * @param role     the role to look for.  If null or an empty string, 
     *                   match the Interface where role is not specified or 
     *                   is set to an empty string.  
     * @param version  the version of the protocol to get; if null, 1.0 is 
     *                   assumed.  If null or an empty string, match the 
     *                   Interface where role is not specified or is set to 
     *                   an empty string.  
     * @param lenient  if true, be lenient in determining which Interface is 
     *                   most likely refers to the desired one.  In particular,
     *                   if there is only one interface and it is not marked 
     *                   with a version, that interface will be returned. 
     * @return Metadata -- the Inteface node as a Metadata instance, or null
     *                   if not matching Inteface was found.
     */
    public Metadata getInterface(String role, String version, boolean lenient) {
        Metadata intf = null;
        String irole = null, ivers = null;

        if (role == null) role = "";
        if (version == null) version = "";

        List interfaces = findBlocks("interface");
        if (interfaces.size() == 1 && lenient) {
            intf = (Metadata) interfaces.get(0);
            irole = intf.getParameter("role");
            if (irole == null) irole = "";
            ivers = intf.getParameter("version");
            if (ivers == null) ivers = "";

            if ((irole.length() == 0 || role.equals(irole)) &&
                (ivers.length() == 0 || version.equals(ivers))  )
            {
                return intf;
            }
        }

        ListIterator iter = interfaces.listIterator();
        while (iter.hasNext()) {
            intf = (Metadata) iter.next();
            irole = intf.getParameter("role");
            if (irole == null) irole = "";
            ivers = intf.getParameter("version");
            if (ivers == null) ivers = "";
            if (role.equals(irole) && version.equals(ivers))
                return intf;
        }

        // implement!
        return null;
    }

    /**
     * return the access URL for the standard interface for this capability
     */
    public String getStandardAccessURL(String version, boolean lenient) {
        if (version == null) version = "1.0";
        return getAccessURL("std", version, lenient);
    }

    /**
     * return the access URL for the standard interface for this capability
     * @param role     the interface role to look for.  If null or an empty 
     *                   string, match the Interface where role is not specified 
     *                   or is set to an empty string.  
     * @param version  the interface version of the protocol to get; if null, 
     *                   1.0 is assumed.  If null or an empty string, match the 
     *                   Interface where role is not specified or is set to 
     *                   an empty string.  
     */
    public String getAccessURL(String role, String version) {
        return getAccessURL(role, version, false);
    }

    /**
     * return the access URL for the standard interface for this capability
     * @param role     the interface role to look for.  If null or an empty 
     *                   string, match the Interface where role is not specified 
     *                   or is set to an empty string.  
     * @param version  the interface version of the protocol to get; if null, 
     *                   1.0 is assumed.  If null or an empty string, match the 
     *                   Interface where role is not specified or is set to 
     *                   an empty string.  
     * @param lenient  if true, be lenient in determining which interface is 
     *                   most likely refers to the desired one.  In particular,
     *                   if there is only one interface and it is not marked 
     *                   with a version, that interface will be returned. 
     */
    public String getAccessURL(String role, String version, boolean lenient) {
        Metadata intf = getInterface(role, version, lenient);
        if (intf == null) return null;
        return intf.getParameter("accessURL");
    }



}
