/*
 * Created by Ray Plante for the National Virtual Observatory
 * as part of ivoaregistry (RI1 search library)
 * c. 2006
 * Adapted for ivoaharvester
 */
package net.ivoa.registry.vores;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Map;

/**
 * a class for extracting VOResource metadata out of a DOM tree.
 */
public class VOResource extends Metadata {

    private String title = null;
    private String identifier = null;
    private String shortName = null;
    private String restype = null;

    /**
     * create a VOResource metadata extracter
     * @param el   the root resource element
     */
    public VOResource(Element el) {
        super(el);
    }

    /**
     * create a VOResource metadata extracter from a Document assuming 
     * particular root element name.  The first element found matching the 
     * element name will be returned.  
     * @param doc         the Document containing a VOResource record.  
     * @param namespace   the namespace URI for the VOResourcer record's root 
     *                      element.
     * @param elname      the name of the VOResourcer record's root element.
     * @return VOResource  the VOResource instance or null if the root element
     *                      is not found.
     */
    public static VOResource fromDoc(Document doc, 
                                     String namespace, String elname) 
    {
        NodeList nodes = doc.getElementsByTagNameNS(namespace, elname);
        if (nodes == null || nodes.getLength() == 0) return null;

        return new VOResource((Element) nodes.item(0));
    }

    /**
     * create a VOResource metadata extracter from a Document assuming 
     * that the root element is the root of the VOResource record.  
     * @param doc         the Document containing a VOResource record.  
     */
    public static VOResource fromDoc(Document doc) {
        return new VOResource(doc.getDocumentElement());
    }

    /**
     * return the resource class.  This is the value of the xsi:type attribute
     * on the VOResource root element.  The namespace prefix is stripped off
     * before returning.  
     */
    public String getResourceClass() {
        if (restype != null) return restype;

        String out = ((Element) getDOMNode()).getAttributeNS(XSI_NS, "type");
        if (out == null) out = "Resource";
        int c = out.indexOf(":");
        if (c >= 0) out = out.substring(c+1);
        restype = out;

        return out;
    }

    void cacheIdentityData(String name) {
        Node child = getDOMNode().getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (title == null && child.getNodeName().equals("title")) {
                    title = getTrimmedElementValue(child);
                    if (name.equals("title")) break;
                }
                else if (shortName == null && 
                         child.getNodeName().equals("shortName")) 
                {
                    shortName = getTrimmedElementValue(child);
                    if (name.equals("shortName")) break;
                }
                else if (identifier == null && 
                         child.getNodeName().equals("identifier")) 
                {
                    identifier = getTrimmedElementValue(child);
                    if (name.equals("identifier")) break;
                }
            }
            child = child.getNextSibling();
        }
    }

    /**
     * return the resource's title
     */
    public String getTitle() {
        if (title == null) cacheIdentityData("title");
        return title;
    }

    /**
     * return the resource's identifier
     */
    public String getIdentifier() {
        if (identifier == null) cacheIdentityData("identifier");
        return identifier;
    }

    /**
     * return the resource's short name
     */
    public String getShortName() {
        if (shortName == null) cacheIdentityData("shortName");
        return shortName;
    }

    /**
     * return all Resource-level validation levels attached to this
     * record
     */
    public Map<String, Integer> getValidationLevels() {
        return Metadata.getValidationLevels(this);
    }

    /**
     * return the Resource-level validation level attached to this record 
     * as assigned by a specified registry.
     * Null is returned if the specified registry did not set a 
     * legal value.
     * @param who   the IVOA Identifier for the registry that set the validation
     *                 level
     */
    public Integer getValidationLevelBy(String who) {
        return Metadata.getValidationLevelBy(this, who);
    }

    /**
     * return the capability element of a specified type.
     * @param type         the value of the xsi:type without a namespace prefix
     * @return Capability  the matching capability element or null if it 
     *                       doesn't exist
     */
    public Capability findCapabilityByType(String type) {
        Metadata[] out = getBlocks("capability");
        if (out == null) return null;

        for(int i=0; i < out.length; i++) {
            String xsitype = out[i].getXSIType();
            if (type.equals(xsitype)) 
                return new Capability(((Element) out[i].getDOMNode()), xsitype,
                                      null);
        }

        return null;
    }

    /**
     * return the capability element of a specified type.
     * @param id           the value of the standardID 
     * @return Capability  the matching capability element or null if it 
     *                       doesn't exist
     */
    public Capability findCapabilityByID(String id) {
        Metadata[] out = getBlocks("capability");
        if (out == null) return null;

        for(int i=0; i < out.length; i++) {
            String stdid = 
                ((Element) out[i].getDOMNode()).getAttribute("standardID");
            if (id.equals(stdid)) 
                return new Capability(((Element) out[i].getDOMNode()), 
                                      null, stdid);
        }

        return null;
    }

}
