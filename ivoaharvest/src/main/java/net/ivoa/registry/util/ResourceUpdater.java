package net.ivoa.registry.util;

import net.ivoa.registry.std.RIStandard;

import ncsa.xml.saxfilter.SAXFilteredReader;
import ncsa.xml.saxfilter.SAXFilterContentHandler;
import ncsa.xml.saxfilter.OnDemandParser;
import ncsa.xml.saxfilter.OnDemandParserDelegate;
import ncsa.xml.saxfilter.SAXFilterFlowControl;
import ncsa.xml.saxfilter.CharContentLocator;
import ncsa.xml.saxfilter.IOinSAXException;
import ncsa.xml.saxfilter.MultiSFContentHandler;
import ncsa.xml.sax.Namespaces;

import java.io.Reader;
import java.io.FileReader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import javax.xml.namespace.QName;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

/**
 * a utility class that will copy a VOResource record, updating the update 
 * date and, optionally, the resource status.
 * <p>
 * In a VOResource XML record, the update date the is stored as the "updated" 
 * attribute of the resource's root element.  The resource status (whose value 
 * is either "active", "inactive", or "deleted") is stored as the "status" 
 * attribute of the same element.  Note that if these do not appear at the 
 * expected location in the input file, they will be added.  
 * <p>
 * By default, the root of the VOResource record is assumed to be the root 
 * element of the document; however, the record can be wrapped in any 
 * arbitrary XML envelope if the VOResource record's root element qualified 
 * name is given either at construction or via 
 * {@link #setResourceRoot(QName) setResourceRoot()}.  In this case, any 
 * number of VOResource record may appear in the input document.  
 */
public class ResourceUpdater {

    public static enum Status {
        /** resource is current available */
        ACTIVE("active"),
        /** resource is current not available */
        INACTIVE("inactive"),
        /** resource has been removed */
        DELETED("deleted");

        private final String v;
        private Status(String s) { v = s; }
        public boolean equalsValue(String value) { 
            return toString().equals(value); 
        }
        public String toString() { return v; }
    }

    QName vorroot = null;
    String upd = null;
    boolean setcreated = false;
    DateFormat fmtr = null;
    Status status = null;
    String defns = null;
    MultiSFContentHandler handlers = new MultiSFContentHandler(new Handler());

    /**
     * create the updater that will just update the update date on a 
     * single resource.  The root element of the document is assumed to 
     * be the VOResource root element.  
     */
    public ResourceUpdater() { }

    /**
     * create the updater that will update the update date and the status.
     */
    public ResourceUpdater(Status status) { 
        setStatus(status);
    }

    /**
     * create the updater that will update the update date and the status.
     * @param status    the status to change the resource to.
     * @param date      the date to set the updated attribute to
     */
    public ResourceUpdater(Status status, Date date) { 
        setStatus(status);
        setUpdateDate(date);
    }

    /**
     * create the updater that will update the update date and the status.
     * @param status      the status to change the resource to.
     * @param date        the date to set the updated attribute to
     * @param setcreated  if true, the created attribute will also be set to
     *                        date.
     */
    public ResourceUpdater(Status status, Date date, boolean setcreated) { 
        this(status, date);
        setCreatedDate(setcreated);
    }

    /**
     * set the element to search to find the root of a VOResource record.
     * @param el   the qualified name of the VOResource root element.  An 
     *                example would 
     *                <code>RIStandard.resourceElementQName()</code>.
     */
    public void setResourceRoot(QName el) {
        vorroot = el;
    }

    /**
     * set the element to search to find the root of a VOResource record.
     * @param nsuri     the element's namespace URI
     * @param elname    the element's localname
     */
    public void setResourceRoot(String nsuri, String elname) {
        setResourceRoot(new QName(nsuri, elname));
    }

    /**
     * set the root element to the Registry Interface standard element
     * as defined by the given standards properties.  
     * @param std   a RIProperties Properties instance that contains the 
     *                RI standard strings.  That is, a Properties instance
     *                returned by {@link RIStandard#getDefaultDefinitions()} or 
     *                {@link RIStandard#getDefintionsFor(String)}.  If null, 
     *                the Properties returned by 
     *                {@link #getDefaultDefinitions()} will be used.  
     */
    public void setResourceRoot(Properties std) {
        setResourceRoot(RIStandard.getRIResourceRoot(std));
    }

    /**
     * set the root element to the Registry Interface standard element
     * as defined by the given standards properties.  
     */
    public void setRIResourceRoot() {
        setResourceRoot(RIStandard.getRIResourceRoot());
    }

    /**
     * set the value that will be inserted as the updated date.  No 
     * checking of the format id done; consider using 
     * {@link #setUpdateDate(Date)}.  
     */
    public void setUpdateDate(String updated) {
        upd = updated;
    }

    /**
     * set the value that will be inserted as the updated date.  No 
     * checking of the format id done; consider using 
     * {@link #setUpdateDate(Date)}.  
     */
    public void setUpdateDate(Date updated) {
        upd = getDateFormat().format(updated);
    }

    /**
     * set the updated date to be the current time.  That is, it will be
     * set to what ever time it is when {@link #update()} is called.  
     */
    public void setUpdateToNow() {
        upd = null;
    }

    /**
     * returnn the date string that will be inserted into updated records
     * Null is returned if the current time will be set.
     */
    public String getUpdateDate() { return upd; }

    /**
     * set the formatter for formatting updated dates
     */
    public void setDateFormat(DateFormat formatter) {
        fmtr = formatter;
    }

    /**
     * return the current instance of the DateFormat that will be used 
     * format updated dates
     */
    public DateFormat getDateFormat() {
        if (fmtr == null) 
            fmtr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        return fmtr;
    }

    /**
     * set whether the created date will also be updated as well.  If true,
     * it will be set to the same date as the updated data.
     */
    public void setCreatedDate(boolean doit) {
        setcreated = doit;
    }

    /**
     * return whether the created date will also be updated as well.  If true,
     * it will be set to the same date as the updated data.
     */
    public boolean settingCreatedDate() { return setcreated; }

    /**
     * set the status to set when updating resource records.  
     * @param stat    the status value to set; if null, the status will 
     *                  not be changed.
     */
    public void setStatus(Status stat) {
        status = stat;  
    }

    /**
     * do not update the status
     */
    public void keepStatus() { setStatus(null); }

    /**
     * return the status that will be set when updating resource records.
     * Null is returned if the value will not be updated.
     */
    public Status getStatus() { return status; }

    /**
     * set the default namespace of on the resource root element to be 
     * the proper namespace for VOResource record contents.
     */
    public void setDefNamespace() {  defns = "";  }

    /**
     * do not update the default namespace on the root element.  Call this 
     * {@link #setDefNamespace()} was previously called.
     */
    public void keepDefNamespace() {  defns = null;  }

    /**
     * return true if this class will be setting default namespace
     */
    public boolean settingDefNamespace() { return defns != null; }

    /**
     * add a ContentHandler that can edit parts of the resource.  To actually
     * edit the resource, the handler must be a SAXFilterContentHandler.
     * generic ContentHandlers can be added, however, to snoop on the 
     * stream while it is being updated.  
     */
    public void addContentHandler(ContentHandler ch) {
        handlers.addHandler(ch);
    }

    /**
     * copy an input XML stream to an output stream, updating it along the 
     * way.  The changes configured into this instance will be applied to 
     * all resource root elements found in the stream.
     * @param in    the source stream containing the XML data to update.  
     *                  It will be read to its end.
     * @param out   the destination stream to write the updated XML to.  It
     *                  will not be closed at the end of the tranfer.
     */
    public void update(Reader in, Writer out) throws IOException {
        Reader instrm = new SAXFilteredReader(in, handlers);
        char[] buf = new char[1024];
        int n = 0;
        while ((n = instrm.read(buf)) >= 0)
            out.write(buf, 0, n);
    }

    /**
     * copy an input XML stream to an output stream, updating it along the 
     * way.  The changes configured into this instance will be applied to 
     * all resource root elements found in the stream.
     * @param in    the file containing the source XML data to update.  
     * @param out   the file to write the updated XML to.  
     */
    public void update(File in, File out) throws IOException {
        FileReader instrm = null;
        FileWriter outstrm = null;
        try {
            instrm = new FileReader(in);
            outstrm = new FileWriter(in);
            update(instrm, outstrm);
        }
        finally {
            if (instrm != null) {
                try { instrm.close(); } catch (IOException ex) { }
            }
            if (outstrm != null) {
                try { outstrm.close(); } catch (IOException ex) { }
            }
        }
    }

    class Handler extends DefaultHandler implements SAXFilterContentHandler {
        SAXFilterFlowControl ctrl = null;
        boolean sawDocRoot = false;
        
        @Override public void setParseRequestMgr(OnDemandParser prm) { 
            prm.enableElement(true);
            prm.enableNamespaces(true);
            prm.enableAttributes(true);
        }
        @Override public void setNamespaces(Namespaces namespaces) { }
        @Override public void setFlowController(SAXFilterFlowControl control) {
            ctrl = control;
        }

        @Override public void startDocument() { sawDocRoot = false; }
        @Override public void endDocument() { ctrl = null; }
        @Override public void startElement(String uri, String localname, 
                                           String qName, Attributes atts) 
            throws SAXException
        {
            boolean isroot = false;
            if (vorroot == null)  
                isroot = ! sawDocRoot;
            else
                isroot = uri.equals(vorroot.getNamespaceURI()) &&
                   localname.equals(vorroot.getLocalPart());

            sawDocRoot = true;
            if (isroot) {
                try {
                    updateRootEl(atts, uri);
                } catch (IOException ex) {
                    throw new IOinSAXException(ex);
                }
            }
        }

        void updateRootEl(Attributes atts, String elns) throws IOException {
            CharContentLocator loc = ctrl.getCharLocator();
            StringBuilder content = new StringBuilder(loc.getContent());

            if (status != null) content = updateStatus(content, atts);

            String date = upd;
            if (date == null) date = getDateFormat().format(new Date());
            content = updateUpdated(date, content, atts);
            if (setcreated) content = updateCreated(date, content, atts);

            if (defns != null) content = updateDefNS(content, elns, atts);

            ctrl.substitute(content.toString(), 
                            loc.getCharNumber(), loc.getCharLength());
        }

        StringBuilder updateStatus(StringBuilder eltext, Attributes atts) {
            return ensureAtt("status", status.toString(), eltext, atts);
        }

        StringBuilder updateUpdated(String date, StringBuilder eltext, 
                                    Attributes atts) {
            return ensureAtt("updated", date, eltext, atts);
        }
        StringBuilder updateCreated(String date, StringBuilder eltext, 
                                    Attributes atts) {
            return ensureAtt("created", date, eltext, atts);
        }
        
        StringBuilder updateDefNS(StringBuilder eltext, String elns,
                                  Attributes atts) 
        {
            // extract the text giving the qualified element name
            int p = 1; // p=0 is '<'
            while (p < eltext.length() &&
                   ! Character.isWhitespace(eltext.charAt(p))) { p++; }
            if (p >= eltext.length()) {
                // empty tag
                if (eltext.charAt(p-1) == '>') p--;
            }
            if (elns != "") {
                String el = eltext.substring(1,p);
                if (! el.contains(":")) {
                    // element name is governed by the default ns
                    eltext.insert(1, "ri:");
                    insertAtt(eltext, "xmlns:ri", elns);
                }
            }

            return ensureAtt("xmlns", "", eltext, atts);
        }

        StringBuilder ensureAtt(String name, String value, 
                                StringBuilder eltext, Attributes atts) 
        {
            String oldval = atts.getValue("", name);
            if (oldval == null) 
                // not found; insert the value
                return insertAtt(eltext, name, value);
            else {
                if (oldval.equals(value)) return eltext;

                // found it; swap its value
                return updateAtt(eltext, name, value);
            }
        }

        private StringBuilder insertAtt(StringBuilder eltext, 
                                        String name, String value)
        {
            StringBuilder newatt = new StringBuilder(name);
            newatt.append("=\"").append(value).append("\" ");

            int p = findFirstAtt(eltext);
            if (eltext.charAt(p) == '>') eltext.insert(p++,' ');
            eltext.insert(p, newatt);
            return eltext;
        }
        private int findFirstAtt(StringBuilder eltext) {
            int p = 1; // p=0 is '<'
            while (p < eltext.length() &&
                   ! Character.isWhitespace(eltext.charAt(p))) { p++; }
            if (p >= eltext.length()) {
                // empty tag
                if (eltext.charAt(p-1) == '>') p--;
                eltext.insert(p++, ' ');
                return p;
            }
            while (p < eltext.length() &&
                   Character.isWhitespace(eltext.charAt(p))) { p++; }
            return p;
        }

        private StringBuilder updateAtt(StringBuilder eltext, 
                                        String name, String value)
        {
            int s = findAttQuoteStart(eltext, name);
            if (s < 3) return insertAtt(eltext, name, value);
            int e = findAttQuoteEnd(eltext, s);
            if (e == s+1) 
                eltext.insert(e, value);
            else 
                eltext.replace(s+1, e, value);
            return eltext;
        }

        private int findAttQuoteStart(StringBuilder eltext, String name) {
            int p = eltext.indexOf(name + "=");
            return p + name.length() + 1;
        }
        private int findAttQuoteEnd(StringBuilder eltext, int qstart) {
            char quote = eltext.charAt(qstart);
            int p = eltext.indexOf(Character.toString(quote), qstart+1);
            if (p < 0) {
                eltext.insert(qstart+1, quote);
                p = qstart+1;
            }
            return p;
        }
    }

}
