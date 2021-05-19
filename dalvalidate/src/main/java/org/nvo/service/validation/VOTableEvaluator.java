package org.nvo.service.validation;

import net.ivoa.util.Configuration;

import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * an Evaluator that will test a VOTable service response by applying an XSL 
 * stylesheet to it.  
 *
 * This class can be configured via a Configuration object.  When passed 
 * to the constructor, it will be searched for stylesheet elements of the 
 * form
 * <pre>
 *    &lt;stylesheet responseType="identifier">stylesheet* 
 * </pre>
 */
public class VOTableEvaluator extends XSLEvaluator {

    /**
     * VOTable v1.1 namespace
     */
    public final static String VOTABLE_V1_1 = 
        "http://www.ivoa.net/xml/VOTable/v1.1";

    /**
     * VOTable v1.0 namespace
     */
    public final static String VOTABLE_V1_0 = 
        "http://www.ivoa.net/xml/VOTable/v1.0";

    /**
     * The string returned by determineResultType() when the input document is 
     * a VOTable v1.1.
     */
    public final static String V1_1_RESULT = "v1.1";

    /**
     * The string returned by determineResultType() when the input document is 
     * a VOTable v1.0.
     */
    public final static String V1_0_RESULT = "v1.0";

    /**
     * The string returned by determineResultType() when the input document is 
     * a VOTable v1.0.
     */
    public final static String DTD_RESULT = "dtd";

    /**
     * The name of the root element for a VOTable: "VOTABLE"
     */
    public final static String VOTABLE_ROOT_ELEMENT = "VOTABLE";

    /**
     * create the evaluator
     * @param transfact   a TransformerFactory object used to create
     *                     new XSL Transoformers
     * @param resClass    the class to use to lookup the location of 
     *                     stylesheets as resources.
     */
    public VOTableEvaluator(TransformerFactory transfact, Class resClass) {
        super(transfact, resClass);
    }

    /**
     * Create an Evaluator. 
     * @param config      an Evaluator configuration block.  
     * @param transfact   a TransformerFactory object used to create
     *                     new XSL Transoformers
     * @param resClass    the class to use to lookup the location of 
     *                     stylesheets as resources.
     */
    public VOTableEvaluator(Configuration config, TransformerFactory transfact, 
                            Class resClass) 
    {
        super(config, transfact, resClass);
    }

    /**
     * examine the response from the service to determine which stylesheet 
     * to apply to it. <p>
     *
     * This implementation will attempt to recognize different standard 
     * VOTable versions.  If this method can recognize the response as a 
     * VOTable, it will return one of the following strings defined as 
     * final static members of this class:
     * <pre>
     *    V1_1_RESULT       version 1.1, defined by an XML Schema document
     *    V1_0_RESULT       version 1.0, defined by an XML Schema document
     *    DTD_RESULT        version 1.0, defined by the standard DTD
     * </pre>
     * This label can be passed to getTransformerFor(String) to retrieve 
     * a parsed stylesheet appropriate for that version of VOTable.  
     * @param serviceResponse   the XML response from the service
     * @return String   the label that should be used to retrieve the 
     *                     stylesheet file name from the configuration.
     * @exception UnrecognizedResultTypeException  if the contents are not
     *     recognized.  This simple implementation never throws this.  
     */
    public String determineResponseType(Document serviceResponse) 
         throws UnrecognizedResponseTypeException
    {
        Element root = serviceResponse.getDocumentElement();
        String ns = root.getNamespaceURI();
        String tag = root.getTagName();
        if (VOTABLE_ROOT_ELEMENT.equals(tag)) {

            if (VOTABLE_V1_1.equals(ns)) return V1_1_RESULT;
            if (VOTABLE_V1_0.equals(ns)) return V1_0_RESULT;
            if (ns == null || ns.length() == 0) return DTD_RESULT;

            throw new UnrecognizedResponseTypeException(
                                    "Not a legal VOTable namespace: " + ns);
        }

        throw 
           new UnrecognizedResponseTypeException("Response is not a VOTable");
    }
}
