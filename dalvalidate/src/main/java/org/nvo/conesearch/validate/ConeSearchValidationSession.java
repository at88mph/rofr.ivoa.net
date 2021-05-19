package org.nvo.conesearch.validate;

import net.ivoa.util.Configuration;
import org.nvo.service.validation.SimpleIVOAServiceValidater;
import org.nvo.service.validation.HTTPGetTestQuery;
import org.nvo.service.validation.ResultTypes;
import org.nvo.service.validation.TestingException;
import org.nvo.service.validation.ValidaterListener;
import org.nvo.service.validation.webapp.SimpleIVOAServiceValidationSession;

import java.util.Properties;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import java.util.Date;
import java.text.DateFormat;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.reflect.Method;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;


/**
 * a validation session handler for ConeSearch services.
 */
public class ConeSearchValidationSession 
    extends SimpleIVOAServiceValidationSession 
{

    // set the type name of this validater
    { setServiceType("ConeSearch"); }

    /**
     * create a session
     * @exception FileNotFoundException   if config is null and the default
     *                configuration file cannot be found
     */
    public ConeSearchValidationSession() throws IOException, SAXException {
        super();
    }

    /**
     * create a session
     * @exception FileNotFoundException   if config is null and the default
     *                configuration file cannot be found
     * @exception SAXException   if there is an XML syntax error in the default
     *                configuration file 
     */
    public ConeSearchValidationSession(TransformerFactory tf, Class resClass) 
         throws IOException, SAXException
    {
        super(tf, resClass);
    }

    /**
     * create a session
     * @exception FileNotFoundException   if config is null and the default
     *                configuration file cannot be found
     * @exception SAXException   if there is an XML syntax error in the default
     *                configuration file 
     */
    public ConeSearchValidationSession(Configuration config, 
                                       TransformerFactory tf, Class resClass) 
         throws IOException, SAXException
    {
        super(config, tf, resClass);
    }

    /**
     * wrap a user-provided service query in a QueryData object.  
     */
    protected HTTPGetTestQuery.QueryData makeQueryData(String query) {
        return new HTTPGetTestQuery.QueryData("user", "user", query,
                                              "user-provided cone");

    }

    /**
     * make an argument string for the service to be validated from the user's
     * inputs.  Return null if a legal string could not be returned.
     * @param params   the Properties object containing the users inputs
     */
    protected String makeUserQuery(Properties params) {
        StringBuffer sb = new StringBuffer();
        Object val = params.get("RA");
        if (val != null) sb.append("RA=").append(val).append('&');
        val = params.get("DEC");
        if (val != null) sb.append("DEC=").append(val).append('&');
        val = params.get("SR");
        if (val != null) sb.append("SR=").append(val).append('&');
        if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

}
