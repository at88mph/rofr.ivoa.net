package net.ivoa.registry.harvest;

import net.ivoa.registry.util.ResourceUpdater;
import net.ivoa.registry.util.ResourceSummary;
import net.ivoa.registry.util.ResourcePeeker;

import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.Writer;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * a {@link net.ivoa.registry.harvest.Repository Repository} implementation 
 * that manages harvested records from a registry persisted in a directory 
 * on a filesystem.  In particular, this class can harvest records from a 
 * registry in full or incrmentally and store the results on disk.
 * <p>
 * The {@link #update(boolean)} function is used to pull in the latest 
 * records from the registry.  It uses the {@link Harvester} class to do 
 * the actual harvesting, providing it with a {@link HarvestConsumer} to 
 * cache the new records to disk.  A {@link HarvestListener} can be provided
 * that can be alerted when harvesting has completed to trigger their ingest.
 * <p>
 * This class is constructed with home directory where it will write the 
 * collected records and internal accounting.  In particular, each collected
 * resource record document will be written with a name of the form, 
 * <code>res<i>###-#</i>.xml</code>.  
 */
public class FSRepository extends Repository {

    protected File cacheDir = null;
    protected File tmpDir = null;
 
    static final String TMPSUBDIR = "_tmp";
    static final String LASTHARVESTFILE = "_lastHarvest";
    static final String NAMELOOKUPFILE = "_nameLookup";
    static final String DELETEDLISTFILE = "_deleted";
    static final String HARVESTDATE = "harvest.date.successful";

    /**
     * create a repository using a given cache directory.
     * @param harvestEndpoint  the base URL endpoint for the harvesting 
     *                  interface to harvest from.
     * @param cache   the directory where resource records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @param clear   if true, clear out any previously cached resource records 
     *                  to start afresh.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public FSRepository(String harvestEndpoint, File cache, boolean clear) 
        throws FileNotFoundException, IOException 
    {
        this(harvestEndpoint, cache, clear, true, null);
    }

    /**
     * create a repository using a given cache directory.
     * @param harvestEndpoint  the base URL endpoint for the harvesting 
     *                  interface to harvest from.
     * @param cache   the directory where resource records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public FSRepository(String harvestEndpoint, File cache) 
        throws FileNotFoundException, IOException 
    {
        this(harvestEndpoint, cache, false);
    }

    /**
     * create a repository using a given cache directory.
     * @param harvestEndpoint  the base URL endpoint for the harvesting 
     *                  interface to harvest from.
     * @param cache   the directory where resource records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @param logger  the Logger to use in this instance.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public FSRepository(String harvestEndpoint, File cache, Logger logger) 
        throws FileNotFoundException, IOException 
    {
        this(harvestEndpoint, cache, false, true, logger);
    }

    /**
     * create a repository using a given cache directory.
     * @param harvestEndpoint  the harvesting service endpoint
     * @param cache   the directory where resource records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @param clear   if true, clear out any previously cached resource records 
     *                  to start afresh.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     * @param logger  the Logger to use in this instance.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public FSRepository(String harvestEndpoint, File cache, boolean clear, 
                        Logger logger) 
        throws FileNotFoundException, IOException 
    {
        this(harvestEndpoint, cache, clear, true, null);
    }

    /**
     * create a repository using a given cache directory.
     * @param harvestEndpoint  the harvesting service endpoint
     * @param cache   the directory where resource records can be cached.  If it 
     *                  does not exist, it will be created unless its parent
     *                  directory does not exist.
     * @param clear   if true, clear out any previously cached resource records 
     *                  to start afresh.
     * @param localOnly  if true, return only records that are originally 
     *                     published with the registry.  If false, all records
     *                     will be returned including those harvested from other
     *                     registries.  
     * @param logger  the Logger to use in this instance.
     * @throws FileNotFoundException  if neither the cache directory nor its
     *                  parent directory exists.
     * @throws IOException  if the the cache directory does not exist and cannot
     *                  be created.
     */
    public FSRepository(String harvestEndpoint, File cache, boolean clear, 
                        boolean localOnly, Logger logger) 
        throws FileNotFoundException, IOException 
    {
        super(harvestEndpoint, localOnly, logger);
        if (cache.getName().length() == 0) 
           throw new IllegalArgumentException("Empty path given for " +
                                              "resource cache");
        cacheDir = cache;
        if (! cacheDir.exists()) {
            File parent = cacheDir.getParentFile();
            if (parent == null) 
                parent = new File(System.getProperty("java.os.dir","."));
            if (! parent.exists())
                throw new FileNotFoundException(parent.toString() + 
                                                ": File not found");
            if (! cacheDir.mkdir())
                throw new IOException("Failed to create cache dir: " + cacheDir);
        }

        tmpDir = new File(cacheDir, TMPSUBDIR);
        if (! tmpDir.exists() && ! tmpDir.mkdir()) 
            throw new IOException("Failed to create tmp dir: " + tmpDir);

        if (clear) {
            logr.info("Clearing all previous resource records as per request");
            clearPersistedResources();
        }
    }

    /**
     * remove all persisted resource records harvested from the registry.
     */
    protected void clearPersistedResources() {
        for(File file : resourceFiles()) {
            if (! file.delete()) {
                logr.warning("Trouble removing resource record: " + 
                             file.getName());
            }
        }
        File lastharvest = new File(cacheDir, LASTHARVESTFILE);
        if (lastharvest.exists()) {
            if (! lastharvest.delete()) {
                logr.warning("Trouble removing "+LASTHARVESTFILE+" file");
            }
        }
    }

    /**
     * return the files containing resource records within this repository
     */
    protected File[] resourceFiles() {
        return resourceFiles(cacheDir);
    }

    /**
     * return the files containing resource records within the given directory.
     * This implementation assumes the storage conventions implemented by this
     * class.   
     * @param dir   the directory containing the resource record files.
     */
    public static File[] resourceFiles(File dir) {
        FilenameFilter filt = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.charAt(0) != '_' && name.endsWith(".xml"));
                }
            };
        return dir.listFiles(filt);
    }

    /**
     * return the OAI-formatted string encodeing the time of the last 
     * harvest that updated this repository.  This is should be the 
     * OAI response timestamp from the first page of harvesting results 
     * received.  
     */
    public String getLastHarvest() throws IOException {
        File lastharvestfile = new File(cacheDir, LASTHARVESTFILE);
        if (! lastharvestfile.exists()) return null;

        Properties status = new Properties();
        status.load(new FileReader(lastharvestfile));
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

    /**
     * update the resource record content of this repository via standard
     * harvesting mechanisms.  This method should call 
     * {@link #createHarvester(boolean) createHarvester()}
     * @param incremental   if true, attempt to get only those records 
     *                          updated since the last harvest.  
     * @returns int   the number of new publisher records written.  
     *                   If incremental is false, this number should be equal
     *                   to the total number available.  
     */
    public synchronized int update(boolean incremental) 
        throws HarvestingException, IOException 
    {
        URL hurl = null;
        try { hurl = new URL(endpointURL); }
        catch (MalformedURLException ex) {
            throw new HarvestingException("Bad harvesting URL: " + endpointURL,
                                          ex, 0);
        }
        Harvester hrvstr = createHarvester(incremental, hurl);
        File outdir = null;
        String lastharvest = getLastHarvest();
        if (incremental && lastharvest != null) hrvstr.setFrom(lastharvest);

        try {
            if (getRegistryName() != null) {
                for(HarvestListener listener : listeners) 
                    listener.harvestInfo(0, null, listener.REGISTRY_NAME, 
                                         getRegistryName());
            }
            int n = hrvstr.harvest(createConsumer(incremental));

            // if not incremental, clear out the old records only if harvesting
            // was successful.  
            if (! incremental) {
                for(File oldfile : resourceFiles(cacheDir)) 
                    oldfile.delete();
            }

            return n;
        } 
        finally {
            if (! incremental) {
                // move the new publisher records into the cache
                for(File resfile : resourceFiles(tmpDir)) {
                    File cacheFile = new File(cacheDir, resfile.getName());
                    if (! resfile.renameTo(cacheFile)) {
                        logr.warning("Failed to move " + outdir.getName() + 
                                     File.separator + cacheFile.getName() + "!");
                    }
                }
            }
            Properties hinfo = hrvstr.getLastHarvestInfo();
            if (lastharvest != null &&
                ! "true".equals(hinfo.getProperty("harvest.complete"))) 
            {
                hinfo.setProperty("harvest.date.successful", lastharvest);
            }
            saveLastHarvest(hinfo);

            for(HarvestListener listener : listeners) 
                listener.harvestInfo(0, null, listener.HARVEST_COMPLETE, 
                                     getRegistryName());
        }
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

    class Consumer implements HarvestConsumer {
        File dir = null;
        Logger logger = Logger.getLogger(logr.getName()+"/harvest");
        ResourcePeeker reser = new ResourcePeeker(logger);
        FileNameLookup flookup = new FileNameLookup();
        public Consumer(File outdir) {
            dir = outdir;
        }
        @Override public void harvestStarting() { }
        @Override public void harvestDone(boolean success) { 
            try { flookup.persist();  }
            catch (IOException ex) {
                logger.warning("Failed to write file lookup file: " + 
                               ex.getMessage());
            }
        }
        @Override public boolean canRestart() { return true; }

        @Override
        public void consume(HarvestedRecord record) throws IOException {
            
            if (record.isAvailable()) {
                File tmpfile = File.createTempFile("_harv",".xml", dir);
                tmpfile.deleteOnExit();
                Writer out = new FileWriter(tmpfile);
                record.writeContent(out);
                out.close();

                // rename it based on its identifier
                File acctd = null;
                try { 
                    String name = makeRecordFilename(record.getID());
                    if (name == null) return;   // shouldn't happen
                    acctd = new File(tmpfile.getParentFile(), name);
                    if (! tmpfile.renameTo(acctd)) {
                        logr.warning("Failed to move temp reg-rec file (" +
                                     tmpfile.getName() + ") to " + 
                                     acctd.getName());
                    }
                }
                finally {
                    if (tmpfile.exists() /* && acctd == null */)
                        tmpfile.delete();
                }
            }
            else if (record.isDeleted()) {
                // updated version not provided; look for existing record
                File old = null;
                String filename = flookup.fileFor(record.getID());
                if (filename != null) {
                    File[] dirs = { dir, cacheDir };
                    for (File ddir : dirs) {
                        old = new File(ddir, filename);
                        if (old.exists()) break;
                        old = null;
                    }
                }

                if (old != null) {
                    ResourceUpdater updr = new ResourceUpdater();
                    updr.setStatus(ResourceUpdater.Status.DELETED);
                    File nw = File.createTempFile("_upd", ".xml", dir);
                    nw.deleteOnExit();
                    String udate = record.getDatestamp();
                    if (udate.endsWith("Z")) 
                        udate = udate.substring(0,udate.length()-1);
                    updr.setUpdateDate(udate);
                    try {
                        updr.update(old, nw);
                        if (nw.exists()) 
                            nw.renameTo(old);
                        else 
                            logr.severe("Failed to update now deleted record " +
                                     "for unknown reason (no output produced)");
                    } catch (IOException ex) {
                        logr.severe("Trouble updating now deleted record: " + 
                                   ex.getMessage());
                        old = null;
                    }
                }

                // no updatable version available; record into special file
                if (old == null) {
                    File outfile = new File(cacheDir, DELETEDLISTFILE);
                    PrintWriter out = 
                        new PrintWriter(new FileWriter(outfile, true));
                    out.println(record.getID());
                    out.close();
                }
            }
            // we will ignore "missing" active records (shouldn't happen).
        }

        String makeRecordFilename(String id) throws IOException {
            String out = flookup.fileFor(id);
            if (out == null) {
                int i = 0;
                out = "res"+Integer.toString(id.hashCode());
                File of = new File(dir, out+'-'+Integer.toString(i)+".xml");
                while (of.exists()) 
                    of = new File(dir, out+'-'+Integer.toString(++i)+".xml");
                out = of.getName();
                flookup.set(id, out);
            }
            return out;
        }
    }

    class FileNameLookup {
        File file = null;
        Properties data = new Properties();
        public FileNameLookup() {  
            this(new File(cacheDir, NAMELOOKUPFILE));  
            if (! file.exists()) {
                try {
                    File[] resfiles = resourceFiles();
                    if (resfiles.length > 0)
                        restore(resfiles, file);
                } catch (IOException ex) {
                    logr.warning("Trouble restoring missing " + NAMELOOKUPFILE +
                                 " file: " + ex.getMessage());
                }
            }
        }
        public FileNameLookup(File persisted) {
            file = persisted;
            if (file.exists()) {
                try { data.load(new FileReader(file)); }
                catch (IOException ex) {
                    throw new IllegalArgumentException("Bad input file, " +
                                                       file.getName() + ": " + 
                                                       ex.getMessage(), ex);
                }
            }
        }
        public void set(String id, String filename) { 
            data.setProperty(id, filename); 
        }
        public String fileFor(String id) { return data.getProperty(id); }
        public void restore(File[] recs, File lookup) throws IOException {
            ResourceSummary res = null;
            ResourcePeeker peeker = new ResourcePeeker();
            for(File rec : recs) {
                res = peeker.load(rec);
                data.setProperty(res.getID(), rec.getName());
            }
            if (lookup != null) data.store(new FileWriter(lookup), null); 
        }
        public void persist() throws IOException { 
            data.store(new FileWriter(file), null); 
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) 
                throw new IllegalArgumentException("missing directory name");
            if (args.length < 1) 
                throw new IllegalArgumentException("missing endpoint and directory name arguments");
            String ep = args[0];
            String dir = args[1];

            Level lev = Level.FINE;
            Logger use = Logger.getLogger("");
            use.getHandlers()[0].setLevel(lev);
            use = Logger.getLogger("Repository");
            use.setLevel(lev);
            Repository repos = new FSRepository(ep, new File(dir), true, 
                                                true, use);
            int n = repos.update(true);
            System.out.println("Collected " + n + " new records");
        }
        catch (IllegalArgumentException ex) {
            System.err.println("FSRepository: " + ex.getMessage());
            System.err.println("Usage: reg-ep dir");
            System.exit(1);
        }
        catch (Exception ex) {
            System.err.println("FSRepository: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(2);
        }
    }
}
