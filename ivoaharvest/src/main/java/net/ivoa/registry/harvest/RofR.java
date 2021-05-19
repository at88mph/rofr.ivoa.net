package net.ivoa.registry.harvest;

import net.ivoa.registry.util.ResourceSummary;
import net.ivoa.registry.util.ResourcePeeker;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.Writer;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Properties;
import java.util.Iterator;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.logging.Logger;
import java.util.logging.Level;

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

import net.ivoa.registry.std.RIStandard;

/**
 * an interface to the Registry of Registries (RofR) and the information 
 * needed to harvest from all known publishing registries.
 */
public class RofR extends FSRepository {

    HashMap<String, PublishingRegistry> regs = 
        new HashMap<String, PublishingRegistry>(8);
    PubRegLoader pubregloader = new PubRegLoader();

    protected Properties std = null;
 
    /**
     * create a view into the known publishers registered with the 
     * RofR
     * @param cache   the directory where RofR records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @param clear   if true, clear out any previously cached RofR records to
     *                  start afresh.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public RofR(File cache, boolean clear) 
        throws FileNotFoundException, IOException 
    {
        this(cache, clear, null, null);
    }

    /**
     * create a view into the known publishers registered with the 
     * RofR
     * @param cache   the directory where RofR records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public RofR(File cache) 
        throws FileNotFoundException, IOException 
    {
        this(cache, true);
    }

    /**
     * create a view into the known publishers registered with the 
     * RofR
     * @param cache   the directory where RofR records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @param logger  the Logger to use in this instance.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public RofR(File cache, Logger logger) 
        throws FileNotFoundException, IOException 
    {
        this(cache, true, logger, null);
    }

    /**
     * create a view into the known publishers registered with the 
     * RofR
     * @param cache   the directory where RofR records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @param clear   if true, clear out any previously cached RofR records to
     *                  start afresh.
     * @param logger  the Logger to use in this instance.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public RofR(File cache, boolean clear, Logger logger) 
        throws FileNotFoundException, IOException 
    {
        this(cache, clear, logger, null);
    }

    /**
     * create a view into the known publishers registered with the 
     * RofR
     * @param cache   the directory where RofR records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @param clear   if true, clear out any previously cached RofR records to
     *                  start afresh.
     * @param logger  the Logger to use in this instance.
     * @param ristd   a set of properties that define details stipulated by
     *                  IVOA standards.  If null, default values are used.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public RofR(File cache, boolean clear, Logger logger, Properties ristd) 
        throws FileNotFoundException, IOException 
    {
        super(rofrEndpoint(null, ristd), cache, false, logger);
        std = ristd;

        if (clear) {
            logr.info("Clearing memory of known publishing registries as per request");
            clearPersistedPublishers();
        }
        else 
            loadpubs();
    }

    static String rofrEndpoint(String rofrEndpoint, Properties ristd) {
        if (rofrEndpoint != null) return rofrEndpoint;

        if (ristd == null) ristd = RofRHarvester.getStdDefs();
        return ristd.getProperty(RIStandard.ROFR_OAI_ENDPOINT);
    }

    /* 
     * read in the publishing registry records currently in the cacheDir
     */
    void loadpubs() {
        File[] files = resourceFiles();
        for(File pubfile : files) {
            cachePubReg(loadPubFile(pubfile));
        }
    }

    /*
     * open up a publishing registry record and load it into memory
     */
    PublishingRegistry loadPubFile(File pubfile) {
        PublishingRegistry pubreg = null;
        try {
            pubreg = pubregloader.loadReg(pubfile);
        }
        catch (IOException ex) {
            logr.warning("Skipping over " + pubfile.getName() + 
                         " because...");
            logr.warning("IO problem while reading: " + 
                         ex.getMessage());
            return null;
        }

        // now load into lookup
        if (pubreg.getID() == null) {
            logr.warning(pubfile.getName() + 
                         ": missing identifier; skipping.");
            return null;
        }

        return pubreg;
    }

    void cachePubReg(PublishingRegistry pubreg) {
        regs.put(nameForPubReg(pubreg), pubreg);
    }

    File[] pubfiles() {
        return pubfiles(cacheDir);
    }
    File[] pubfiles(File dir) {
        FilenameFilter filt = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.charAt(0) != '_' && name.endsWith(".xml"));
                }
            };
        return dir.listFiles(filt);
    }

    /**
     * clear all memory (including persisted) of known publishing registries
     */
    public void clearPublishers() {
        logr.info("Clearing memory of known publishing registries as per request");
        clearPersistedPublishers();
        regs.clear();
    }

    void clearPersistedPublishers() {
        for(File file : pubfiles()) {
            if (! file.delete()) {
                logr.warning("Trouble removing registry record: " + 
                             file.getName());
            }
        }
    }

    /**
     * create the harvester that should be used to update the resource 
     * record content of this repository.  
     * @param incremental  if true, prepare a harvester that will only update 
     *                        the records that have changed since the last 
     *                        harvest.  This implementation ignores this 
     *                        parameter. 
     */
    protected Harvester createHarvester(boolean incremental, URL harvestURL)
        throws IOException
    {
        Harvester out = new RofRHarvester();
        for(HarvestListener listener : listeners) 
            out.addHarvestListener(listener);
        return out;
    }

    /**
     * create the consumer that should be used to persist the harvested records.
     * @param incremental  if true, prepare a harvester that will only update 
     *                        the records that have changed since the last 
     *                        harvest.  
     */
    protected HarvestConsumer createConsumer(boolean incremental) {
        File outdir = (incremental) ? cacheDir : tmpDir;
        return new Consumer(outdir);
    }

    /**
     * update the cached list of publishing registries
     * @param incremental  if true, only update the publisher list based on 
     *                     changes since the last harvest.  
     * @returns int   the number of new publisher records written.  
     *                   If incremental is false, this number should be equal
     *                   to the total number available.  
     */
    public int updatePublishers(boolean incremental) 
        throws HarvestingException, IOException 
    {
        return update(incremental);
    }

    public String getLastHarvest() throws IOException {
        File lastharvestfile = new File(cacheDir, LASTHARVESTFILE);
        if (! lastharvestfile.exists()) return null;

        Properties status = new Properties();
        status.load(new FileInputStream(lastharvestfile));
        return status.getProperty(HARVESTDATE);
    }

    void saveLastHarvest(Properties info) {
        File outfile = null;
        try {
            outfile = new File(cacheDir, LASTHARVESTFILE);
            FileWriter out = new FileWriter(outfile);
            info.store(out, "Last harvest info");
            out.close();
        }
        catch (IOException ex) {
            logr.warning("Failed to save last harvest info to " + outfile);
        }
    }

    String pubFileNameFor(PublishingRegistry pubreg) {
        return nameForPubReg(pubreg)+".xml";
    }

    String pubFileNameForID(String id) {
        return nameForPubRegID(id)+".xml";
    }

    /**
     * return the default name that will be given to a particular registry
     */
    public String nameForPubReg(PublishingRegistry pubreg) {
        String id = pubreg.getID();
        if (id == null) {
            logr.warning("registry record identifer unexpectedly missing");
            return null;
        }
        return nameForPubRegID(id);
    }

    /**
     * return the default name that will be given to a particular registry
     * based on its ID.
     */
    protected String nameForPubRegID(String id) {
        Pattern schmpat = Pattern.compile("^\\w+:/+");
        Matcher schm = schmpat.matcher(id);
        if (schm.find()) 
            id = schm.replaceFirst("");
        int pos = id.indexOf("/");
        String auth = (pos > 0) ? id.substring(0,pos) : id;
        return auth;
    }

    class Consumer implements HarvestConsumer {
        File dir = null;
        public Consumer(File outdir) {
            dir = outdir;
        }
        @Override public void harvestStarting() { }
        @Override public void harvestDone(boolean success) {  }
        @Override public boolean canRestart() { return true; }

        @Override 
        public void consume(HarvestedRecord record) throws IOException {
            PublishingRegistry pubreg = null;
            File tmpfile = null, acctd = null;
            try {
                if (! record.isDeleted() && record.isAvailable()) {
                    // cache record to temporary file
                    tmpfile = File.createTempFile("_harv",".xml", dir);
                    tmpfile.deleteOnExit();
                    Writer out = new FileWriter(tmpfile);
                    record.writeContent(out);
                    out.close();

                    // load its contents and examine
                    pubreg = loadPubFile(tmpfile);
                    if (pubreg != null && 
                        ! "active".equals(pubreg.getStatus()))
                    {
                        // deleted or inactive: don't use it
                        pubreg = null;
                    }
                }

                if (pubreg != null) {
                    // an active publishing registry
                    // cache it in memory
                    cachePubReg(pubreg);

                    // rename the file to one based on the ID
                    String name = pubFileNameFor(pubreg);
                    if (name == null) return;   // shouldn't happen
                    acctd = new File(tmpfile.getParentFile(), name);
                
                    if (! tmpfile.renameTo(acctd)) {
                        logr.warning("Failed to move temp reg-rec file (" +
                                     tmpfile.getName() + ") to " + 
                                     acctd.getName());
                    }
                }
                else {
                    // deleted or inactive: remove this publisher from our set
                    // ... in memory
                    regs.remove(nameForPubRegID(record.getID()));

                    // ... cached on disk
                    File[] dirs = { dir, cacheDir };
                    File pubrec = null;
                    for (File ddir : dirs) {
                      pubrec = new File(ddir,pubFileNameForID(record.getID()));
                      if (pubrec.exists() && ! pubrec.delete()) 
                          logr.warning("Failed to delete record for deleted " +
                                       "registry: " + pubrec.toString());
                    }
                }
            }
            finally {
                if (tmpfile.exists() /* && acctd == null */)
                    tmpfile.delete();
            }
        }
    }

    /**
     * return the set of publishing registries known to the RofR
     */
    public Set<PublishingRegistry> getPublishers() {
        HashSet<PublishingRegistry> out = 
            new HashSet<PublishingRegistry>(regs.size());
        for(PublishingRegistry reg : regs.values()) {
            out.add(reg);
        }
        return out;
    }

    /**
     * return the set of names for the known registries.  These names do not 
     * not necessarily correspond to either the registry IDs or the shortNames; 
     * however, they are assumed to be unique.  
     */
    public Set<String> getPublisherNames() {
        return new HashSet<String>(regs.keySet());
    }

    /**
     * return the PublishingRegistry summary of the record assigned the 
     * given name.  Null is returned if the name is not recognized. 
     */
    public PublishingRegistry getPublisher(String name) {
        return regs.get(name);
    }

    /**
     * return the number publishing registries currently known to the RofR.
     * This may change after a call to 
     * {@link #updatePublishers(boolean) updatePublishers()}.
     */
    public int getPublisherCount() {
        return regs.size();
    }

    class PubRegLoader extends ResourcePeeker {
        XPathExpression epxp = null;
        public PubRegLoader() {
            super(RofR.this.logr);
            try {
                epxp = xp.compile("/*/capability[contains(@xsi:type,':Harvest')]/interface[@role='std' and contains(@xsi:type,':OAIHTTP')]/accessURL");
            }
            catch (XPathExpressionException ex) {
                // shouldn't happen
                throw new InternalError("Programmer error: " +
                                        "XPathExpressionException: "
                                        + ex.getMessage());
            }
        }

        protected ResourceSummary createResourceSummary() {
            return new PublishingRegistry();
        }

        protected void load(Document doc, ResourceSummary out, File source) 
            throws XPathExpressionException, SAXException
        {
            super.load(doc, out, source);
            ((PublishingRegistry) out).oaiep =  epxp.evaluate(doc);
        }

        public PublishingRegistry loadReg(File resfile) throws IOException {
            return (PublishingRegistry) load(resfile);
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length < 1) 
                throw new IllegalArgumentException("missing directory name");
            String dir = args[0];

            Logger use = Logger.getLogger("RofR");
            use.setLevel(Level.INFO);
            RofR rofr = new RofR(new File(dir), false, use);
            rofr.updatePublishers(true);

            for(PublishingRegistry reg : rofr.getPublishers()) {
                System.out.print(reg.getTitle());
                System.out.println(" (" + reg.getShortName() + ")");
                System.out.println(" ID: " + reg.getID());
                System.out.println(" EP: " + reg.getHarvestingEndpoint());
                System.out.println(" Last Updated: " + reg.getUpdatedDate());
                System.out.println(" Last Harvested: " + reg.getHarvestedDate());
            }
        }
        catch (IllegalArgumentException ex) {
            System.err.println("RofR: " + ex.getMessage());
            System.err.println("Usage: rofr dir");
            System.exit(1);
        }
        catch (Exception ex) {
            System.err.println("RofR: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(2);
        }
    }
}

class VORegNamspaces implements NamespaceContext {
    Properties ns = new Properties();

    public VORegNamspaces() {
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
