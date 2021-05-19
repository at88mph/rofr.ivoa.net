package org.nvo.service.validation;

import net.ivoa.util.Configuration;

import javax.xml.transform.TransformerFactory;
import javax.xml.parsers.DocumentBuilder; 

import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * A class that uses XSL to apply tests to a VOTable response from a test
 * query.  This can be used to test VO "Simple" services (e.g. ConeSearch,
 * Simple Image Access, etc.).  
 * 
 * A Configuration should be used to set the stylesheets to support the 
 * different versions of VOTable.  For example, the following can be 
 * used for checking Cone Service services:
 * <pre>
 *   <stylesheet responseType="dtd">checkConeSearch.xsl</stylesheet>
 *   <stylesheet responseType="v1.0">checkConeSearch-v1_0.xsl</stylesheet>
 *   <stylesheet responseType="v1.1">checkConeSearch-v1_1.xsl</stylesheet>
 * </pre>
 * The three response types configured map respectively to the original 
 * DTD-based version and the two XML Schema-based versions, v1.0 and v1.1.  
 * This class has been implemented to recognize each of these as well as 
 * any later version of VOTable (assuming the namespace follows the same
 * pattern as v1.1).  
 *
 * A stylesheet directory can also be set in the configuration to direct
 * this class to look for the stylesheets in a particular location; otherwise,
 * it will be assumed that they can be loaded in as resources.  See 
 * {@link XSLEvaluator} for details.  The defaultResponseType normally 
 * should not be set as the stylesheets are not likely to produce 
 * meaningful results. 
 */
public class VOTableXSLEvaluator extends XSLEvaluator {

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
     * Create an Evaluator.  The caller will need to provide a stylesheet
     * by calling {@link #useStylesheet(String,String) useStylesheet()}.  
     */
    public VOTableXSLEvaluator() {
        super();
    }

    /**
     * Create an Evaluator.  The caller will need to provide a stylesheet
     * by calling {@link #useStylesheet(String,String) useStylesheet()}.  
     * @param transfact   a TransformerFactory object used to create
     *                     new XSL Transoformers
     * @param resClass    the class to use to lookup the location of 
     *                     stylesheets as resources.
     */
    public VOTableXSLEvaluator(TransformerFactory transfact, Class resClass) {
        super(transfact, resClass);
    }

    /**
     * Create a configured Evaluator.  The provided Configuration object 
     * should set the stylesheets to use for the reponse type keys, "dtd", 
     * "v1.0" and "v1.0", corresponding to the three versions VOTable 
     * supported.
     * @param config      an Evaluator configuration block.  
     * @param transfact   a TransformerFactory object used to create
     *                     new XSL Transoformers
     * @param resClass    the class to use to lookup the location of 
     *                     stylesheets as resources.
     */
    public VOTableXSLEvaluator(Configuration config, 
                               TransformerFactory transfact, Class resClass) 
    {
        super(config, transfact, resClass); 
    }

    /**
     * examine the response from the service to determine which stylesheet 
     * to apply to it. <p>
     *
     * Specific service validators should override this method.  This 
     * default implementation does not look at the response but simply
     * returns an empty string.  
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
        String tag = root.getLocalName();
        if (VOTABLE_ROOT_ELEMENT.equals(tag)) {

            if (ns == null || ns.length() == 0) return DTD_RESULT;

            if (VOTABLE_V1_1.equals(ns)) return V1_1_RESULT;
            if (VOTABLE_V1_0.equals(ns)) return V1_0_RESULT;

            // Support any future version of VOTable assuming that the 
            // the namespace will use the same base URL as v1.1.  
            String future = futureVersion(ns);
            if (future != null) return future;

            throw new UnrecognizedResponseTypeException(
                                    "Not a legal VOTable namespace: " + ns);
        }

        throw new UnrecognizedResponseTypeException("Result is not a VOTable");
    }

    /**
     * if desired, set the ErrorHandler for the parser.  This implementation
     * does not set an ErrorHandler and returns null;
     * @param db       the DOM parser
     * @param query    the TestQuery being handled
     * @return ParsingErrors  the ErrorHandler object that was set.
     */
    public ParsingErrors setParsingErrorHandler(DocumentBuilder db,
                                                TestQuery query)
    {
        ParsingErrors out = new ParsingErrors();
        db.setErrorHandler(out);
        return out;
    }

    private String futureVersion(String ns) {
        int sl = VOTABLE_V1_1.lastIndexOf('/');
        if (sl < 0) return null;

        String base = VOTABLE_V1_1.substring(0,sl+1);
        if (ns.startsWith(base)) 
            return ns.substring(base.length());
        else 
            return null;
    }

}
