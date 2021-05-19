package net.ivoa.registry.util;

import javax.xml.namespace.QName;

/**
 * an interface that can be made aware of the identity of a VOResource record's
 * root element.  With VOResource records, the root node that contains the 
 * VOResource metadata, can be set by the application (it need only have a
 * an XSD type that derives from the VOResource base type).  Through this 
 * interface one can the identity of the root element via a QName.
 * Note that VOResource record root need not be the root of the XML document 
 * (e.g. a document can contain several records wrapped in an arbitrary 
 * envelope).  However, this interface allows one to indicate that the root
 * of the XML document should be assumed to be the root of the VOResource 
 * record--i.e. the document contains a single record with no wrapper/envelope. 
 */
public interface VOResourceRootAware {

    /**
     * set the element to search to find the root of a VOResource record.
     * @param el   the qualified name of the VOResource root element.  An 
     *                example would 
     *                <code>RIStandard.resourceElementQName()</code>.  If 
     *                null, assume the document's root element is the root
     *                of the VOResource record.
     */
    public void setResourceRoot(QName el);

    /**
     * return the element to search to find the root of a VOResource record.
     * @return QName -- the qualified name of the VOResource root element or 
     *                  null if the document's root element is assumed to be 
     *                  the root of the VOResource record.
     */
    public QName getResourceRoot();

    /**
     * return true if the XML document's root element is assumed to be the 
     * root of the VOResource record.  In other words, true means that the 
     * document contains a single VOResource record with no outer wrapper or 
     * envelope.  
     */
    public boolean isVOResourceDoc();
}