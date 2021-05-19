package net.ivoa.registry.harvest;

import net.ivoa.registry.std.RIStandard;
import net.ivoa.registry.std.RIProperties;

import java.net.URL;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * a tool for harvesting VOResource records from an IVOA publishing registry.
 * Given a publishing registry's harvesting URL endpoint, this class provides 
 * pull over successive pages of resource records and hand them off to a
 * {@link HarvestConsumer}.  That is, it will automatically make
 * use of OAI resumption tokens to make multiple calls to the registry until
 * all records have be retrieved.  
 */
public class Harvester implements RIProperties {

    URL oaiURL = null;
    String from = null;
    String until = null;
    Set sets = new HashSet();

    HashSet<HarvestListener> listeners = new HashSet<HarvestListener>();
    String lastHarvestDate = null;
    boolean lastHarvestComplete = false;
    int lastHarvestCount = 0;
    String lastHarvestRequest = null;
    String lastHarvestURL = null;
    Properties recHarvestInfo = null;  

    protected Logger logr = null;
    protected Properties std = null;
    static Properties defstd = null;

    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.  Only locally published records
     * will be returned.  
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     */
    public Harvester(URL endpoint) {
        this(endpoint, true);
    }
    
    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.  Only locally published records
     * will be returned.  
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     */
    public Harvester(URL endpoint, Logger logger) {
        this(endpoint, true, logger);
    }
    
    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     */
    public Harvester(URL endpoint, boolean localOnly, Logger logger) {
        this(endpoint, localOnly, false, null, logger);
    }

    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     */
    public Harvester(URL endpoint, boolean localOnly) {
        this(endpoint, localOnly, false);
    }

    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     * @param embedHarvestInfo  if true, the harvested records will have extra
     *                     data about the harvesting encoded as a processing
     *                     instruction at the top of the record.  
     * @param logger     the logger to use 
     */
    public Harvester(URL endpoint, boolean localOnly, 
                     boolean embedHarvestInfo, Logger logger) 
    {
        this(endpoint, localOnly, embedHarvestInfo, null, logger);
    }

    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     * @param embedHarvestInfo  if true, the harvested records will have extra
     *                     data about the harvesting encoded as a processing
     *                     instruction at the top of the record.  
     */
    public Harvester(URL endpoint, boolean localOnly, 
                     boolean embedHarvestInfo) 
    {
        this(endpoint, localOnly, embedHarvestInfo, null, null);
    }

    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     * @param embedHarvestInfo  if true, the harvested records will have extra
     *                     data about the harvesting encoded as a processing
     *                     instruction at the top of the record.  
     * @param ristd      a set of properties that define details stipulated by
     *                     IVOA standards.  If null, default values are used.
     * @param logger     the logger to use 
     */
    public Harvester(URL endpoint, boolean localOnly, 
                     boolean embedHarvestInfo, Properties ristd)
    {
        this(endpoint,localOnly, embedHarvestInfo, ristd, null);
    }

    /**
     * create a harvester ready to harvest from a publishing registry with 
     * a given harvesting service endpoint.
     * @param endpoint   the base URL to the OAI-PMH interface of the publishing
     *                     registry.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     * @param embedHarvestInfo  if true, the harvested records will have extra
     *                     data about the harvesting encoded as a processing
     *                     instruction at the top of the record.  
     * @param ristd      a set of properties that define details stipulated by
     *                     IVOA standards.  If null, default values are used.
     * @param logger     the logger to use 
     */
    public Harvester(URL endpoint, boolean localOnly, 
                     boolean embedHarvestInfo, Properties ristd, 
                     Logger logger) 
    {
        oaiURL = endpoint;
        if (ristd == null) ristd = getStdDefs();
        std = ristd;
        if (localOnly) addSetName(std.getProperty(MANAGED_OAISET));
        if (embedHarvestInfo) {
            recHarvestInfo = new Properties();
            recHarvestInfo.setProperty("from.ep", oaiURL.toString());
        }

        if (logger == null) logger = Logger.getLogger(getClass().getName());
        logr = logger;
    }

    static Properties getStdDefs() {
        if (defstd == null) 
            defstd = RIStandard.getDefaultDefinitions();
        return defstd;
    }

    /**
     * set a property that should be included as a harvestInfo property.
     * These properties will be inserted into a processing-instruction
     * called "harvestInfo" just after the XML declaration within each 
     * output.  If this class was not instantiated with 
     * embedHarvestInfo=true, added properties are ignored.
     */
    public void addHarvestProperty(String name, String value) {
        if (recHarvestInfo != null) recHarvestInfo.setProperty(name, value);
    }

    /**
     * set the ID of the registry we are harvesting from.  This will be 
     * ignored unless this class was instantiated with embedHarvestInfo=true.
     */
    public void setRegistryID(String id) {
        addHarvestProperty("from.id", id);
    }

    /**
     * set the date to harvest since.  Only records created or updated since
     * this date will be returned.  This must be an ISO8601 formatted timestamp 
     * string.
     */
    public void setFrom(String date) { 
        from = date; 
        addHarvestProperty("since", date);
    }
    
    /**
     * set the date to harvest until.  Only records created or updated before
     * this date will be returned.  This must be an ISO8601 formatted timestamp 
     * string.
     */
    public void setUntil(String date) { until = date; }
    
    /**
     * return the date to harvest since.  Only records created or updated since
     * this date will be returned.  This will be an ISO8601 formatted timestamp 
     * string.
     */
    public String getFrom() { return from; }
    
    /**
     * return the date to harvest until.  Only records created or updated before
     * this date will be returned.  This will be an ISO8601 formatted timestamp 
     * string.
     */
    public String setUntil() { return until; }

    /**
     * add a set name to restrict the harvesting
     */
    public void addSetName(String set) {  sets.add(set);  }

    /**
     * return the sets that will be retrieved;
     */
    public Iterator sets() { return sets.iterator(); }

    /**
     * return the base URL to the OAI service that will be harvested from
     */
    public URL getBaseURL() { return oaiURL; }

    /**
     * return the URL to return a page of records from the registry.
     * @param tok    a valid resumption token value returned by a previous 
     *                 harvesting call.
     */
    public URL getListURL(String tok) {
        String base = oaiURL.toString();
        StringBuilder sb = new StringBuilder(base);
        if (base.charAt(base.length()-1) != '?' &&
            base.charAt(base.length()-1) != '&') 
        {
            sb.append( (base.indexOf("?") < 0) ? '?' : '&' );
        }
        sb.append("verb=ListRecords&");

        if (tok == null) 
            // URL for first page of records
            sb.append(getListRecordsArgs());
        else {
            try { tok = URLEncoder.encode(tok,"UTF-8"); }
            catch (UnsupportedEncodingException ex) { /* shouldn't happen */ }
            sb.append("resumptionToken=").append(tok);
        }

        try {
            return new URL(sb.toString());
        }
        catch (MalformedURLException ex) {
            // should not happen
            throw new InternalError("programmer error? bad URL: " + 
                                    ex.getMessage() + ": " + sb.toString());
        }
    }
    
    /**
     * return the full ListRecords URL that will be used to harvest.
     */
    public String getListRecordsArgs() {
        String format = std.getProperty(VORESOURCE_OAIFORMAT);
        String base = oaiURL.toString();
        StringBuffer out = new StringBuffer();
        /*
         * will include this bit in the baseurl
        if (base.charAt(base.length()-1) != '?' && 
            base.charAt(base.length()-1) != '&')
        {
            out.append((base.indexOf("?") < 0) ? '?' : '&');
        }
        out.append("verb=ListRecords&");
        */
        out.append("metadataPrefix=").append(format);

        Iterator i = sets.iterator(); 
        if (i.hasNext()) {
            out.append("&set=");
            while (i.hasNext()) {
                out.append(i.next());
                if (i.hasNext()) out.append(':');
            }
        }

        if (from != null) out.append("&from=").append(from);
        if (until != null) out.append("&until=").append(until);
        return out.toString();
    }

    /**
     * accept a harvest listener to the harvesting process
     */
    public void addHarvestListener(HarvestListener listener) {
        if (listeners == null) listeners = new HashSet<HarvestListener>();
        listeners.add(listener);
    }

    /**
     * remove a harvest listener to the harvesting process
     */
    public void removeHarvestListener(HarvestListener listener) {
        if (listeners == null) return;
        listeners.remove(listener);
    }

    /**
     * return Properties that describe the last harvesting process,
     * including the harvesting date.
     */
    public Properties getLastHarvestInfo() {
        Properties out = new Properties();
        out.setProperty("harvest.endpoint", oaiURL.toString());
        out.setProperty("harvest.complete", Boolean.FALSE.toString());
        if (lastHarvestRequest != null) 
            out.setProperty("harvest.request", lastHarvestRequest);
        if (lastHarvestURL != null) 
            out.setProperty("harvest.request.lastURL", lastHarvestURL);
        if (lastHarvestDate != null) {
            out.setProperty("harvest.date.attempted", lastHarvestDate);
            if (lastHarvestComplete) 
                out.setProperty("harvest.date.successful", lastHarvestDate);
            out.setProperty("harvest.complete", 
                            Boolean.toString(lastHarvestComplete));
        }
        return out;
    }

    class MyHarvestListener implements HarvestListener {
        public void tellWantedItems(Set<String> names) {
            names.add(RESPONSE_DATE);
        }
        @Override
        public void harvestInfo(int page, String id, 
                                String itemName, String value)
        {
            if (page <= 1 && RESPONSE_DATE.equals(itemName))
                lastHarvestDate = value;
        }
    }

    InputStream openStream(URL hurl) throws HarvestingException {
        try { return hurl.openStream(); }
        catch (IOException ex) { 
            throw new 
                HarvestingException("Harvesting service connection failed: "
                                    + ex.getMessage(), ex, 0);
        }
    }

    /**
     * harvest from the registry and cache all of the records to a directory
     * @param directory    the directory to cache the VOResource files into
     * @param basename     a basename to form the output filenames.  Each
     *                       VOResource files will be called 
     *                   <i>basename</i><code>_</code><i>#</i><code>.xml</code>,
     *                       where <i>#</i> is an integer.  
     * @return int   the number of records harvested.
     */
    public int harvestToDir(File directory, String basename) 
        throws HarvestingException, IOException
    {
        HarvestConsumer dest = new CacheToDirectory(directory, basename);
        return harvest(dest);
    }

    /**
     * harvest from the registry.  
     * @param consumer   a HarvestConsumer to send the harvested records to
     * @return int       the number of records harvested.
     */
    public int harvest(HarvestConsumer consumer) 
        throws HarvestingException, IOException
    {
        Writer out = null;
        Reader in = null;
        char[] buf = new char[16*1024];

        int n = 0;
        lastHarvestCount = 0;
        lastHarvestComplete = false;
        lastHarvestDate = null;

        ResumptionToken tok = null;
        int page = 1;
        RecordResponseProcessor feeder = 
            new RecordResponseProcessor(consumer, recHarvestInfo);
        feeder.addListener(new MyHarvestListener());
        feeder.addListeners(listeners);

        try {
            URL listurl = getListURL(null);
            lastHarvestRequest = lastHarvestURL = listurl.toString();

            consumer.harvestStarting();
            for(HarvestListener listener : listeners)
                listener.harvestInfo(page, null, listener.REQUEST_URL, 
                                     lastHarvestURL);

            // process the first page of records
            tok = feeder.process(openStream(listurl), page);
            if (logr.isLoggable(Level.FINE)) {
                String msg = "Processed initial page of records";
                if (tok != null && tok.moreRecords()) msg += " ("+tok+")";
                logr.fine(msg);
            }

            while (tok != null && tok.moreRecords()) {
                listurl = getListURL(tok.getTokenValue());
                lastHarvestURL = listurl.toString();
                tok = feeder.process(openStream(listurl), ++page);
                if (logr.isLoggable(Level.FINE)) {
                    String msg = "Processed page " + Integer.toString(page) + 
                        " of the records";
                    if (tok != null && tok.moreRecords())
                        msg += " (" + tok + ")";
                    logr.fine(msg);
                }
            }
            consumer.harvestDone(true);
            lastHarvestComplete = true;
        }
        catch (HarvestingException ex) {
            consumer.harvestDone(false);
            tellListenersDone(false);
            ex.setCompletedRecordCount(feeder.getProcessedCount());
            ex.setRequestURL(lastHarvestURL);
            throw ex;
        }
        catch (IOException ex) {
            consumer.harvestDone(false);
            tellListenersDone(false);
            throw ex;
        }
        catch (RuntimeException ex) {
            consumer.harvestDone(false);
            tellListenersDone(false);
            throw ex;
        }
        finally {
            lastHarvestCount = feeder.getProcessedCount();
        }

        return lastHarvestCount;
    }

    private void tellListenersDone(boolean okay) {
        String value = (okay) ? "true" : "false";
        for (HarvestListener listener : listeners) {
            listener.harvestInfo(0, null, listener.HARVEST_COMPLETE, value);
        }
    }

    /**
     * a simple command-line application interface to harvesting.  This 
     * will harvest from the registry harvest URL given as the first argument
     * and cache the VOResource files to a directory given as the second 
     * argument.  If a third argument is provided, it will be used as the 
     * output file basename.
     * @see #harvestToDir(File, String)
     */
    public static void main(String[] args) {
        try {
            if (args.length <= 0) 
                throw new IllegalArgumentException("missing baseURL");
            String baseURL = args[0];

            if (args.length <= 1) 
                throw new IllegalArgumentException("missing directory name");
            String dir = args[1];

            String basename = "vor";
            if (args.length > 2) basename = args[2];

            Harvester h = new Harvester(new URL(baseURL));
            int nrecs = h.harvestToDir(new File(dir), basename);

            System.out.println("Harvested " + nrecs + " records into " + 
                               dir + ".");
        }
        catch (IllegalArgumentException ex) {
            System.err.println("Harvester: " + ex.getMessage());
            System.err.println("Usage: harvestreg baseURL dir [ basename ]");
            System.exit(1);
        }
        catch (Exception ex) {
            System.err.println("Harvester: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(2);
        }
    }
}
