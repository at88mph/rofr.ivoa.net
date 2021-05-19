package org.nvo.conesearch.validate;

import net.ivoa.util.Configuration;
import org.nvo.service.validation.SimpleIVOAServiceValidater;
import org.nvo.service.validation.HTTPGetTestQuery;
import org.nvo.service.validation.ResultTypes;
import org.nvo.service.validation.TestingException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;

import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class ConeSearchValidaterServlet extends HttpServlet {

    static Object lock = new Object();
    static SimpleIVOAServiceValidater validater = null;
    static TransformerFactory tf = null;
    static Hashtable props = new Hashtable();

    static {
        Class thiscl = ConeSearchValidaterServlet.class;
        Method[] methods = thiscl.getMethods();
        for(int i=0; i < methods.length; i++) {
            Class[] params = methods[i].getParameterTypes();
            if (methods[i].getName().startsWith("set") && params.length == 1 
                && params[0].equals(String.class)) 
            {
                props.put(methods[i].getName().substring(3), methods[i]);
            }
        }
    }

    private String baseURL = null;
    private String ra = null;
    private String dec = null;
    private String sr = null;
    private String format = XML_FORMAT;
    private Vector problems = new Vector();
    private ResultTypes rt = new ResultTypes(0);

    public final static String HTML_FORMAT = "html";
    public final static String XML_FORMAT = "xml";
    public final static String VOTABLE_FORMAT = "votable";

    public ConeSearchValidaterServlet() { }

    /**
     * set the base URL of the Cone Search service to test
     */
    public void setBASEURL(String baseUrl) { 
        baseURL = correctBaseURL(baseUrl); 
    }

    /**
     * return the base URL of the Cone Search service to test
     */
    public String getBASEURL() { return baseURL; }

    /**
     * set the right ascension to use in the test query
     */
    public void setRA(String RA) { ra = RA; }

    /**
     * return the right ascension to use in the test query
     */
    public String getRA() { return ra; }

    /**
     * set the declination to use in the test query
     */
    public void setDEC(String DEC) { dec = DEC; }

    /**
     * set the declination to use in the test query
     */
    public String getDEC() { return dec; }

    /**
     * set the search radius to use in the test query
     */
    public void setSR(String SR) { sr = SR; }

    /**
     * set the search radius to use in the test query
     */
    public String getSR() { return sr; }

    /**
     * set the format for the result returned.  
     */
    public void setFORMAT(String FORMAT) { format = FORMAT.toLowerCase(); }

    /**
     * return the format for the result returned.  
     */
    public String getFORMAT() { return format; }

    public static String correctBaseURL(String url) {
        url = url.trim();
        char lastChar = url.charAt(url.length()-1);
        boolean hasQM = url.indexOf('?') > 0;
        if (hasQM && lastChar != '?' && lastChar != '&') 
            return url + '&';
        else if (!hasQM)
            return url + '?';
        else 
            return url;
    }

    /**
     * throw an exception if the inputs are illegal in some way
     */
    public boolean isOk() {
        double val;
        boolean ok = true;
        problems.removeAllElements();

        if (ra == null || ra.length() == 0) {
            ok = false;
            problems.addElement("Please provide right ascension (RA) value");
        }
        else {
            try {  val = Double.parseDouble(ra);  }
            catch (NumberFormatException ex) {
                ok = false;
                problems.addElement("right ascension (RA) must be within " + 
                                    "[0,360]: " + ra);
            }
        }

        if (dec == null || dec.length() == 0) {
            ok = false;
            problems.addElement("Please provide declination (DEC) value");
        }
        else {
            try {  val = Double.parseDouble(dec);  }
            catch (NumberFormatException ex) {
              ok = false;
              problems.addElement("Declination (DEC) must be within [-90,90]: " 
                                  + dec);
            }
        }

        if (sr == null || sr.length() == 0) {
            ok = false;
            problems.addElement("Please provide search radius (SR) value");
        }
        else {
            try {  val = Double.parseDouble(sr);  }
            catch (NumberFormatException ex) {
                ok = false;
                problems.addElement("Search radius (SR) must be > 0: " + sr);
            }
        }

        if (format == null || format.length() == 0 || 
            (! format.equals(HTML_FORMAT) && ! format.equals(XML_FORMAT) && 
             ! format.equals(VOTABLE_FORMAT)))
        {
            problems.addElement("Unrecognized format: " + format);
        }

        return ok;
    }

    /**
     * return the problems encountered by the last call to isOk() as an
     * array of Strings.
     */
    public String[] getProblems() {
        return (String[]) problems.toArray(new String[problems.size()]);
    }

    /**
     * return the problems encountered by the last call to isOk() as a
     * multi-line string.
     */
    public String getErrorMessage() {
        StringBuffer out = new StringBuffer();
        for(Enumeration e=problems.elements(); e.hasMoreElements();) {
            out.append(e.nextElement());
            if (e.hasMoreElements()) out.append('\n');
        }
        return out.toString();
    }

    /**
     * create a query string from the inputs
     */
    public String getQuery() throws IllegalArgumentException {
        if (! isOk()) 
            throw new IllegalArgumentException(getErrorMessage());

        return "RA="+ra+"&DEC="+dec+"&SR="+sr;
    }

    private String decode(String in) {
        try {
            return URLDecoder.decode(in, "UTF-8");
        }
        catch (UnsupportedEncodingException ex) { return in; }
    }

    /**
     * set the properties based on the input query string
     */
    public boolean setInputs(String qstr) {
        System.out.println("ConeSearchValidatorServlet invoked with: " + 
                           decode(qstr));

        StringTokenizer st = new StringTokenizer(qstr,"&");
        while (st.hasMoreTokens()) {
            String arg = st.nextToken();
            int eq = arg.indexOf("=");
            if (eq > 0 && eq < arg.length()-1) {
                String name = decode(arg.substring(0,eq).trim());

                if (name.equalsIgnoreCase("show")) {
                    rt.addTypes(decode(arg.substring(eq+1)));
                }
                else {
                    Method set = (Method) props.get(name);
                    if (set != null) {
                        try {
                            set.invoke(this, 
                                new Object[] { decode(arg.substring(eq+1)) });
                        }
                        catch (Exception ex) { 
                        
                        }
                    }
                }
            }
        }

        return isOk();
    }

    /**
     * run the validation and write the XML results to a stream
     */
    public Document validate() 
         throws TestingException, DOMException, 
                IOException, ParserConfigurationException, SAXException,
                TransformerConfigurationException, TransformerException
    {
        if (validater == null) {
            synchronized (lock) {
                if (validater == null) {
                    String configfile = 
                        System.getProperty("validation.configfile", 
                                           "config.xml");
                    Configuration config = 
                        new Configuration(configfile, getClass());

                    validater = new SimpleIVOAServiceValidater(config,tf,null);
                }
            }
        }

        HTTPGetTestQuery.QueryData[] userquery = {
            new HTTPGetTestQuery.QueryData("user", "cone", getQuery(), null)
        };

        return validater.validate(baseURL, userquery, null);
    }

    /**
     * do a Cone Search validation via a GET request
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
         throws ServletException, IOException
    {

    }
}

