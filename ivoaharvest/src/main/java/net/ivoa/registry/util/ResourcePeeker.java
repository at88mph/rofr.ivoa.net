package net.ivoa.registry.util;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Iterator;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.namespace.NamespaceContext;
import javax.xml.XMLConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * a utility class that will open up harvested resource files and extract 
 * summary information.  
 */
public class ResourcePeeker {
    protected DocumentBuilder db = null;
    protected XPath xp = null;
    protected VONamespaces namespaces = new VONamespaces();
    XPathExpression updatedxp = null;
    XPathExpression shortnamexp = null;
    XPathExpression titlexp = null;
    XPathExpression idxp = null;
    XPathExpression statusxp = null;
    XPathExpression hinfoxp = null;
    SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Logger logr = null;

    public ResourcePeeker() { this(null); }
    public ResourcePeeker(Logger logger) {
        logr = logger;
        if (logr == null) logr = Logger.getLogger(getClass().getName());

        try {
            DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
            df.setNamespaceAware(true);
            db = df.newDocumentBuilder();
            
            XPathFactory xf = XPathFactory.newInstance();
            xp = xf.newXPath();
            xp.setNamespaceContext(namespaces);
            updatedxp = xp.compile("/*/@updated");
            shortnamexp = xp.compile("/*/shortName");
            titlexp = xp.compile("/*/title");
            idxp = xp.compile("/*/identifier");
            statusxp = xp.compile("/*/@status");
            hinfoxp = xp.compile("/child::processing-instruction('harvestInfo')");
        }
        catch (XPathExpressionException ex) {
            // shouldn't happen
            throw new InternalError("Programmer error: " +
                                    "XPathExpressionException: "
                                    + ex.getMessage());
        }
        catch (ParserConfigurationException ex) {
            // shouldn't happen
            throw new InternalError("XML parsing config error: " + 
                                    ex.getMessage());
        }
    }

    protected ResourceSummary createResourceSummary() {
        return new ResourceSummary();
    }

    public ResourceSummary load(File resfile) throws IOException {
        Document doc = null;
        ResourceSummary out = createResourceSummary();

        // extract info
        try {
            doc = db.parse(resfile);
            load(doc, out, resfile);
        }
        catch (XPathExpressionException ex) {
            // shouldn't happen
            throw new InternalError("xpath expression error: " + 
                                    ex.getMessage());
        }
        catch (SAXException ex) {
            throw new IOException("Error during parsing: "+ex.getMessage(), ex);
        }

        return out;
    }

    protected void load(Document doc, ResourceSummary out, File source) 
        throws XPathExpressionException, SAXException
    {
        String datestr = null;
        Pattern subsec = Pattern.compile("\\.\\d+$");
        Properties hinfo = null;

        out.title = titlexp.evaluate(doc);
        out.shortName = shortnamexp.evaluate(doc);
        out.id = idxp.evaluate(doc);
        out.status = statusxp.evaluate(doc);
        
        hinfo = parseHarvestInfo(hinfoxp.evaluate(doc));
        if (hinfo != null) {
            out.harvestedFromID = hinfo.getProperty("from.id");
            out.harvestedFromEP = hinfo.getProperty("from.ep");

            datestr = hinfo.getProperty("date");
            if (datestr != null && datestr.length() > 0) {
                if (datestr.charAt(datestr.length()-1) == 'Z') 
                    datestr = datestr.substring(0, datestr.length()-1);
                datestr = subsec.matcher(datestr).replaceFirst("");
                
                try { out.harvested = datefmt.parse(datestr); }
                catch (ParseException ex) { 
                    logr.warning("Trouble parsing harvest date in " + 
                                 source.getName() + ": " + datestr);
                    out.harvested = null; 
                }
            }
        }

        datestr = updatedxp.evaluate(doc); 
        if (datestr != null && datestr.length() > 0) {
            // should not have Z, but we will be tolerant here
            if (datestr.charAt(datestr.length()-1) == 'Z') 
                datestr = datestr.substring(0, datestr.length()-1);
            datestr = subsec.matcher(datestr).replaceFirst("");
            try { out.updated = datefmt.parse(datestr); }
            catch (ParseException ex) { 
                logr.warning("Trouble parsing harvest date in " + 
                             source.getName() + ": " + datestr);
                out.updated = null; 
            }
        }
    }

    Properties parseHarvestInfo(String pitext) {
        if (pitext == null || pitext.length() == 0) return null;
        Properties out = new Properties();
        char first, last, quote;

        for(String nameval : pitext.split("\\s+")) {
            String[] nv = nameval.split("=", 2);
            if (nv.length != 2) continue;

            first = nv[1].charAt(0);
            last = nv[1].charAt(nv[1].length()-1);
            if (first == '"' || first == '\'') {
                quote = first;
                if (last == quote) nv[1] = nv[1].substring(1, nv[1].length()-1);
            }

            out.setProperty(nv[0], nv[1]);
        }
        if (out.size() == 0) return null;
        return out;
    }

    public static void main(String[] args) {
        try {
            ResourcePeeker rp = new ResourcePeeker();
            if (args.length < 1) 
                throw new IllegalArgumentException("missing file name");
            String rfile = args[0];

            ResourceSummary reg = rp.load(new File(rfile));
            System.out.print(reg.getTitle());
            System.out.println(" (" + reg.getShortName() + ")");
            System.out.println(" ID: " + reg.getID());
            System.out.println(" From: " + reg.getHarvestedFromEndPoint());
            System.out.println(" RegID: " + reg.getHarvestedFromID());
            System.out.println(" Last Updated: " + reg.getUpdatedDate());
            System.out.println(" Last Harvested: " + reg.getHarvestedDate());

        }
        catch (Exception ex) {
            System.err.println("ResourcePeeker: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(2);
        }
    }    
}

class VONamespaces implements NamespaceContext {
    Properties ns = new Properties();

    public VONamespaces() {
        ns.setProperty("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    }

    public String getNamespaceURI(String prefix) {
        return ns.getProperty(prefix);
    }

    public String getPrefix(String uri) {
        throw new UnsupportedOperationException("getPrefix()");
    }
    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException("getPrefixes()");
    }
}

