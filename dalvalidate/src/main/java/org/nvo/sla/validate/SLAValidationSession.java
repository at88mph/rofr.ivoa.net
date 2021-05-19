package org.nvo.sla.validate;

import net.ivoa.util.Configuration;
import org.nvo.service.validation.SimpleIVOAServiceValidater;
import org.nvo.service.validation.HTTPGetTestQuery;
import org.nvo.service.validation.ResultTypes;
import org.nvo.service.validation.TestingException;
import org.nvo.service.validation.ValidaterListener;
import org.nvo.service.validation.webapp.SimpleIVOAServiceValidationSession;

import java.util.StringTokenizer;
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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
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
 * a validation session handler for SLA services.
 */
public class SLAValidationSession 
    extends SimpleIVOAServiceValidationSession 
{

    // set the type name of this validater
    { setServiceType("SLA"); }

    /**
     * create a session
     * @exception FileNotFoundException   if config is null and the default
     *                configuration file cannot be found
     */
    public SLAValidationSession() throws IOException, SAXException {
        super();
    }

    /**
     * create a session
     * @exception FileNotFoundException   if config is null and the default
     *                configuration file cannot be found
     * @exception SAXException   if there is an XML syntax error in the default
     *                configuration file 
     */
    public SLAValidationSession(TransformerFactory tf, Class resClass) 
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
    public SLAValidationSession(Configuration config, 
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
                                              "user-provided region");

    }

    /**
     * make an argument string for the service to be validated from the user's
     * inputs.  Return null if a legal string could not be returned.
     * @param params   the Properties object containing the users inputs
     */
    protected String makeUserQuery(Properties params) {
        StringBuffer sb = new StringBuffer();

        sb.append("REQUEST=").append(params.getProperty("REQUEST","queryData"));

        sb.append("&WAVELENGTH=").append(params.get("WLMIN")).append('/');
        sb.append(params.get("WLMAX"));

        String other = params.getProperty("FORMAT");
        if (other != null && other.length() > 0) 
            sb.append("&FORMAT=").append(other);

        other = params.getProperty("EXTRAPARAMS");
        if (other != null && other.length() > 0) {
            sb.append('&');
            StringTokenizer st = new StringTokenizer(other);
            while(st.hasMoreTokens()) {
                sb.append(encode(st.nextToken().trim()));
                if (st.hasMoreTokens()) sb.append('&');
            }
        }

        return sb.toString();
    }

    private String encode(String in) {
        try {
            String overencoded = URLEncoder.encode(in, "UTF-8");
            int len = overencoded.length();
            StringBuffer out = new StringBuffer(overencoded.length());
            int i = 0;
            while (i < len) {
                if (overencoded.charAt(i) == '%') {
                    i++;
                    StringBuffer cb = new StringBuffer(2);
                    cb.append(overencoded.charAt(i++));
                    cb.append(overencoded.charAt(i++));
                    String code = cb.toString();
                    if (code.equals("3D") || code.equals("3d")) {
                        out.append('=');
                    }
                    else if (code.equals("26")) {
                        out.append('&');
                    }
                    else if (code.equals("2F") || code.equals("2f")) {
                        out.append('/');
                    }
                    else {
                        out.append('%').append(code);
                    }
                }
                else {
                    out.append(overencoded.charAt(i++));
                }
            }
            System.err.println("encoded str: " + out.toString());
            return out.toString();
        }
        catch (UnsupportedEncodingException ex) { return in; }
    }

}
