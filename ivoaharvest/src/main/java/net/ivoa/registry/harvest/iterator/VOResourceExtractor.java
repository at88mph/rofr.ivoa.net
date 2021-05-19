package net.ivoa.registry.harvest.iterator;

import net.ivoa.registry.std.RIStandard;
import net.ivoa.registry.std.RIProperties;
import net.ivoa.registry.harvest.OAIPMHException;
import net.ivoa.registry.harvest.HarvestingException;
import net.ivoa.registry.harvest.HarvestListener;
import net.ivoa.registry.harvest.ResumptionToken;

import ncsa.xml.extractor.ExtractingParser;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.NoSuchElementException;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.Writer;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

/**
 *  a class for extracting VOResource records out of an OAI-PMH ListRecords
 *  response.  
 *  <p>
 *  To get the records, the client uses the {@link DocumentIterator} 
 *  interface, {@link #nextReader()} or {@link #nextDocument()},
 *  to step through the available records.  When one of these methods return 
 *  null, no more records are available.  
 *  <p>
 *  As a side effect, this class will also look for a resumption
 *  token in the response and makes it available via 
 *  {@link #getResumptionToken()}.  Note, however, that because the token 
 *  usually appears at the end of the input ListRecords response, the token 
 *  is typically not available until {@link #nextReader()} returns null.
 */
public class VOResourceExtractor extends DocumentIteratorBase 
    implements RIProperties 
{
    Reader instrm = null;
    Properties ristd = null;
    ExtractingParser parser = null;
    ResumptionToken resume = null;
    OAIPMHException failure = null;
    String oains = null;
    Properties harvestInfo = null;
    int page = 1;
    LinkedList<HarvestListener> listeners = new LinkedList<HarvestListener>();

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     */
    public VOResourceExtractor(InputStream is) {
        this(is, null);
    }

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     */
    public VOResourceExtractor(Reader is) {
        this(is, null);
    }

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     * @param riStd  the properties containing definitions pertaining to the 
     *                 IVOA Registry Interfaces standard.  This must include
     *                 values for OAI_NAMESPACE, REGISTRY_INTERFACE_NAMESPACE,
     *                 and RESOURCE_ELEMENT.
     */
    public VOResourceExtractor(InputStream is, Properties riStd) {
        this(new InputStreamReader(is), riStd);
    }

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     * @param riStd  the properties containing definitions pertaining to the 
     *                 IVOA Registry Interfaces standard.  This must include
     *                 values for OAI_NAMESPACE, REGISTRY_INTERFACE_NAMESPACE,
     *                 and RESOURCE_ELEMENT.
     * @param page   the page number of results that this extractor will 
     *                 operate on.  Normally, this is set to 1 unless multiple
     *                 pages of results are processed via a resumption token.
     */
    public VOResourceExtractor(InputStream is, Properties riStd, int page) {
        this(new InputStreamReader(is), false, riStd, page);
    }

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     * @param withHarvestInfo  if true, extracted harvest data will be inserted 
     *                 into a "harvestInfo" processing instruction just after 
     *                 the XML declaration within each output record.  
     * @param riStd  the properties containing definitions pertaining to the 
     *                 IVOA Registry Interfaces standard.  This must include
     *                 values for OAI_NAMESPACE, REGISTRY_INTERFACE_NAMESPACE,
     *                 and RESOURCE_ELEMENT.
     * @param page   the page number of results that this extractor will 
     *                 operate on.  Normally, this is set to 1 unless multiple
     *                 pages of results are processed via a resumption token.
     */
    public VOResourceExtractor(InputStream is, boolean withHarvestInfo, 
                               Properties riStd, int page) 
    {
        this(new InputStreamReader(is), withHarvestInfo, riStd, page);
    }

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     * @param withHarvestInfo  if true, extracted harvest data will be inserted 
     *                 into a "harvestInfo" processing instruction just after 
     *                 the XML declaration within each output record.  
     * @param riStd  the properties containing definitions pertaining to the 
     *                 IVOA Registry Interfaces standard.  This must include
     *                 values for OAI_NAMESPACE, REGISTRY_INTERFACE_NAMESPACE,
     *                 and RESOURCE_ELEMENT.
     * @param page   the page number of results that this extractor will 
     *                 operate on.  Normally, this is set to 1 unless multiple
     *                 pages of results are processed via a resumption token.
     */
    public VOResourceExtractor(InputStream is, Properties harvestProps, 
                               Properties riStd, int page) 
    {
        this(new InputStreamReader(is), harvestProps, riStd, page);
    }

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     * @param riStd  the properties containing definitions pertaining to the 
     *                 IVOA Registry Interfaces standard.  This must include
     *                 values for OAI_NAMESPACE, REGISTRY_INTERFACE_NAMESPACE,
     *                 and RESOURCE_ELEMENT.
     */
    public VOResourceExtractor(Reader is, Properties riStd) { 
        instrm = is;
        if (riStd == null) riStd = RIStandard.getDefaultDefinitions();
        ristd = riStd;
        oains = riStd.getProperty(OAI_NAMESPACE);
        parser = null;
    }

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     * @param riStd  the properties containing definitions pertaining to the 
     *                 IVOA Registry Interfaces standard.  This must include
     *                 values for OAI_NAMESPACE, REGISTRY_INTERFACE_NAMESPACE,
     *                 and RESOURCE_ELEMENT.
     */
    public VOResourceExtractor(Reader is, Properties riStd, int page) {
        this(is, false, riStd, page);
    }

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     * @param withHarvestInfo  if true, extracted harvest data will be inserted 
     *                 into a "harvestInfo" processing instruction just after 
     *                 the XML declaration within each output record.  
     * @param riStd  the properties containing definitions pertaining to the 
     *                 IVOA Registry Interfaces standard.  This must include
     *                 values for OAI_NAMESPACE, REGISTRY_INTERFACE_NAMESPACE,
     *                 and RESOURCE_ELEMENT.
     */
    public VOResourceExtractor(Reader is, boolean withHarvestInfo, 
                               Properties riStd, int page) 
    {
        this(is, riStd);
        if (withHarvestInfo) harvestInfo = new Properties();
        this.page = page;
    }

    /**
     * create an instance that will extract records from the given stream
     * @param is     the stream providing the ListRecords response
     * @param harvestProps  if non-null, extracted harvest data will be inserted 
     *                 into a "harvestInfo" processing instruction just after 
     *                 the XML declaration within each output record.  The 
     *                 properties given here will be included.  
     * @param riStd  the properties containing definitions pertaining to the 
     *                 IVOA Registry Interfaces standard.  This must include
     *                 values for OAI_NAMESPACE, REGISTRY_INTERFACE_NAMESPACE,
     *                 and RESOURCE_ELEMENT.
     */
    public VOResourceExtractor(Reader is, Properties harvestProps, 
                               Properties riStd, int page) 
    {
        this(is, riStd);
        if (harvestProps != null) 
            harvestInfo = new Properties(harvestProps);
        this.page = page;
    }

    /**
     * set a property that should be included as a harvestInfo property.
     * These properties will be inserted into a processing-instruction
     * called "harvestInfo" just after the XML declaration within each 
     * output.  If this class was not instantiated with 
     * withHarvestInfo=true, added properties are ignored.
     */
    public void addHarvestProperty(String name, String value) {
        if (harvestInfo != null) harvestInfo.setProperty(name, value);
    }

    /**
     * add a HarvestListener to receive select OAI-PMH data from the stream.
     */
    public void addListener(HarvestListener listener) {
        listeners.add(listener);
    }

    /**
     * add a collection of listeners
     */
    public void addListeners(Collection<HarvestListener> morelisteners) {
        for(HarvestListener listener : morelisteners) {
            addListener(listener);
        }
    }

    protected ExtractingParser createParser() {
        ExtractingParser out = null;
        if (harvestInfo != null) { 
            out = new IVOVORParser(instrm);
        }
        else {
            out = new ExtractingParser(instrm);
        }
        return out;
    }

    void initParser() {
        MultiContentHandler ch = new MultiContentHandler();
        ch.addHandler(new ResumptionFinder());
        ch.addHandler(new Snooper());
        ch.addHandler(new OAIErrorDetector());

        parser = createParser();
        parser.setContentHandler(ch);
        parser.ignoreNamespace(ristd.getProperty(OAI_NAMESPACE));
        parser.ignoreNamespace(ristd.getProperty(OAI_DC_NAMESPACE));
        parser.extractElement(ristd.getProperty(REGISTRY_INTERFACE_NAMESPACE), 
                              ristd.getProperty(RESOURCE_ELEMENT));
    }

    class IVOVORParser extends ExtractingParser {
        public IVOVORParser(Reader is) {
            super(is);
        }
        public String getExportProlog() {
            StringBuilder buf = new StringBuilder("<?harvestInfo ");
            for(Enumeration e=harvestInfo.propertyNames(); e.hasMoreElements();){
                String name = (String) e.nextElement();
                buf.append(name).append("=\"");
                buf.append(harvestInfo.getProperty(name,"")).append("\" ");
            }
            buf.append("?>\n");
            return buf.toString();
        }
    }

    /**
     * return the next document in the set as a Reader object.
     * @exception IOException   if an error occurs while creating Reader to data
     */
    public Reader nextReader() throws HarvestingException, IOException {
        if (parser == null) initParser();
        Reader out = parser.nextNode();
        if (out == null && failure != null) 
            // check for OAI errors
            throw failure;
        return out;
    }

    /**
     * return the resumption token encoded in the response.  Null is 
     * returned if no resumption token element was specified.  Because
     * the resumptionToken element is found at the end of the ListRecords
     * response, this should normally should only be called after all 
     * documents have been read and {@link #nextReader()} returns null.  
     */
    public ResumptionToken getResumptionToken() { return resume; }

    /**
     * return true if a resumption token element was specific and includes 
     * an actual token.
     */
    public boolean shouldResume() { 
        return (resume != null && resume.moreRecords()); 
    }

    /*
     * This ContentHandler looks for the OAI resumption token
     */
    class ResumptionFinder extends DefaultHandler {
        // ResumptionToken tok = null;
        String expdate = null;
        String size = null; 
        String tok = null;
        String cursor = null;
        final String oaiuri = oains;
        final String resumption = "resumptionToken";

        public void startDocument() {
            tok = null;
        }

        public void startElement(String uri, String localname, String qName,
                                 Attributes atts) 
            throws SAXException
        {
            if (localname.equals(resumption) /* && oaiuri.equals(uri) */) {
                tok = "";
                expdate = atts.getValue("expirationDate");
                String val = null;
                try {
                    val = atts.getValue("completeListSize");
                    if (val != null) size = val;
                }
                catch (NumberFormatException ex) { /* be tolerant */ }
                try {
                    val = atts.getValue("cursor");
                    if (val != null) cursor = val;
                }
                catch (NumberFormatException ex) { /* be tolerant */ }
            }
        }

        public void characters(char[] text, int start, int length) {
            if (tok != null) {
                String rt = new String(text);
                tok = tok+rt;
            }
        }

        public void endElement(String uri, String localname, String qName)
            throws SAXException
        {
            if (tok != null && 
                localname.equals(resumption) && oaiuri.equals(uri)) 
            {
                resume = new ResumptionToken(tok, expdate, size, cursor);
                tok = size = cursor = expdate = null;
            }
        }
    }

    /*
     * this ContentHandler feeds information to HarvestListeners.  
     */
    class OAIErrorDetector extends DefaultHandler {
        MultiContentHandler container = null;
        String code = null;
        String message = null;
        final String oaiuri = oains;
        final String errorEl = "error";
        final String listRecordsEl = "ListRecords";
        public OAIErrorDetector() { this(null); }
        public OAIErrorDetector(MultiContentHandler c) { 
            container = c;
        }
        public void startDocument() { code = null; message = null; }
        public void startElement(String uri, String localname, String qName,
                                 Attributes atts) 
        {
            // some servers (HEASARC, I'm looking at you) are not qualifying
            // the error element properly
            // if (localname.equals(errorEl) && oaiuri.equals(uri)) {
            if (localname.equals(errorEl) && atts.getValue("code") != null) {
                code = atts.getValue("code");
                if (code == null || code.length() == 0) 
                    code = "unknown";
                else if (code.equals("noRecordsMatch")) 
                    code = null;
            }
            else if (localname.equals(listRecordsEl) && oaiuri.equals(uri)) {
                if (container != null) container.removeHandler(this);
            }
        }
        public void characters(char[] text, int start, int length) {
            if (code != null) {
                String txt = new String(text);
                message = (message == null) ? txt : message+txt;
            }
        }
        public void endElement(String uri, String localname, String qName)
            throws SAXException
        {
            if (code != null) {
                if (localname.equals(errorEl)) {
                    failure = new OAIPMHException(code, message);
                    if (container != null) container.removeHandler(this);
                }
            }
        }
    }


    /*
     * this ContentHandler feeds information to HarvestListeners.  
     */
    class Snooper extends DefaultHandler {
        LinkedList<HarvestListener> listeners = 
            new LinkedList<HarvestListener>();
        String name = null, value = null;
        final String oaiuri = oains;
        final String responseDate = "responseDate";

        boolean deleted = false;
        final String rins = ristd.getProperty(REGISTRY_INTERFACE_NAMESPACE),
                     riel = ristd.getProperty(RESOURCE_ELEMENT);

        public Snooper() {
            HashSet<String> need = new HashSet<String>();
            for(HarvestListener listener : VOResourceExtractor.this.listeners) {
                need.clear();
                listener.tellWantedItems(need);
                if (need.contains(HarvestListener.RESPONSE_DATE))
                    listeners.add(listener);
            }
        }
        public void startDocument() { name = null; }
        public void startElement(String uri, String localname, String qName,
                                 Attributes atts) 
        {
            // check for RESPONSE_DATE
            if (localname.equals(responseDate) && oaiuri.equals(uri)) {
                name = HarvestListener.RESPONSE_DATE;
            }
        }
        public void characters(char[] text, int start, int length) {
            if (name != null) {
                String txt = new String(text);
                value = (value == null) ? txt : value+txt;
            }
        }
        public void endElement(String uri, String localname, String qName)
            throws SAXException
        {
            if (name != null) {
                if (localname.equals(responseDate) && oaiuri.equals(uri)) {
                    // fix the date for those who get it wrong
                    value = fixDate(value);

                    // save this for the harvestInfo prolog
                    if (harvestInfo != null) 
                        harvestInfo.setProperty("date", value);
                    for(HarvestListener listener : listeners) 
                        listener.harvestInfo(page, null, name, value);
                }
                name = null;
            }
        }

        final Pattern tz = Pattern.compile("\\s*[+\\-](\\d\\d:?\\d\\d)Z?$");
        final Pattern subsec = Pattern.compile("\\.\\d+Z?$");
        final Pattern dateof = Pattern.compile("^\\d{4}\\-\\d{2}-\\d{2}");
        final Pattern noTsep = Pattern.compile("(?<=\\-\\d{2})\\s+(?=\\d{2}:)");
        final Pattern correct = 
            Pattern.compile("^\\d{4}\\-\\d\\d-\\d\\d(T\\d\\d:\\d\\d:\\d\\dZ)?$");
        public String fixDate(String date) {
            if (correct.matcher(date).find())
                // Yeah!
                return date;
            if (! dateof.matcher(date).find()) 
                // oh well, what can we do?
                return date;

            // look for a timezone.  It's suppose to be in UTC; however,
            // we will assume that if they gave a local time, they're 
            // assuming local time as the from= argument.  
            Matcher m = tz.matcher(date);
            if (m.find()) date = m.replaceFirst("");

            m = subsec.matcher(date);
            if (m.find()) date = m.replaceFirst("Z");

            if (date.charAt(date.length()-1) != 'Z')
                date += "Z";

            m = noTsep.matcher(date);
            if (m.find()) date = m.replaceFirst("T");

            return date;
        }
    }

    /*
     * this ContentHandler multiplexes to multiple delegate ContentHandlers
     */
    class MultiContentHandler extends DefaultHandler {
        LinkedList<ContentHandler> handlers = new LinkedList<ContentHandler>();
        public void addHandler(ContentHandler ch) { handlers.add(ch); }
        public void removeHandler(ContentHandler ch) { handlers.remove(ch); }
        public void startDocument() throws SAXException {
            for(ContentHandler handler : handlers) 
                handler.startDocument();
        }
        public void startElement(String uri, String localname, String qName,
                                 Attributes atts) 
            throws SAXException
        {
            for(ContentHandler handler : handlers) 
                handler.startElement(uri, localname, qName, atts);
        }
        public void characters(char[] text, int start, int length) 
            throws SAXException
        {
            for(ContentHandler handler : handlers) 
                handler.characters(text, start, length);
        }
        public void endElement(String uri, String localname, String qName)
            throws SAXException
        {
            for(ContentHandler handler : handlers) 
                handler.endElement(uri, localname, qName);
        }
    }

    public static void main(String[] args) {
        try {
            File f = new File(args[0]);
            VOResourceExtractor ext = 
                new VOResourceExtractor(new FileReader(f));

            Reader vor = null;
            char[] buf = new char[16*1024];
            int n;
            Writer out = new OutputStreamWriter(System.out);
            while ((vor = ext.nextReader()) != null) {
                while ((n = vor.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

//     /**
//      * create an extractor that understands with version 1.0 of the Registry 
//      * Interface
//      */
//     public VOResourceExtractor extractorFor10(Reader is) {
//         Properties p = new Properties();
//         RIStandard.loadProperties(p, "1.0");
//         return new VOResourceExtractor(is, p);
//     }

//     /**
//      * create an extractor that understands with version 1.0 of the Registry 
//      * Interface
//      */
//     public VOResourceExtractor extractorFor10(InputStream is) {
//         return extractorFor10(new InputStreamReader());
//     }
}
