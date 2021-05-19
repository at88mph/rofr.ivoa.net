package net.ivoa.registry.harvest;

import java.util.Properties;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.LinkedList;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.StringBufferInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

import net.ivoa.registry.std.RIStandard;
import net.ivoa.registry.validate.VOResourceValidater;
import org.nvo.service.validation.ResultTypes;
import org.nvo.service.validation.TestingException;

/**
 * a location for storing and managing resource records harvested from 
 * all known publishing registries.
 * <p>
 * This class implements the recommendation outlined in the IVOA Note,
 * "The Registry of Registries" (RofR), for harvesting the entire VO.  This can 
 * be accomplished via {@link #update(boolean, int, boolean) update()} which
 * implements the complete process, or one may use the other methods to carry
 * out the individual steps explicitly.  Results are stored on disk in 
 * directories segregated by the source of the harvested records.  The records 
 * can be validated; if requested, this will be done after all harvesting 
 * from a registry is complete.  One can also provide this class with a 
 * {@link HarvestListener} which will be alerted when harvesting has finished 
 * from each known publishing registry. 
 * <p>
 * Harvested records are written to disk below a root directory set at 
 * construction.  Each registry discovered from the RofR will have its own 
 * home subdirectory.  For each registry, the harvested records will be 
 * written to a "<code>harvested</code>" subdirectory (managed internally 
 * by an {@link FSRepository} instance).  If validation is requested, then 
 * after harvesting is completed, each recod is tested for standards 
 * compliance; if it passes, it will get moved to a "<code>valid</code>" 
 * subdirectory (parallel to "<code>harvested</code>"); otherwise, it will 
 * move to an "<code>invalid</code>" subdirectory.  An XML-formated report
 * of the validation results are written to a "<code>valreports</code>"
 * subdirectory.  
 */
public class FullRepository {

    File cacheDir = null;
    RofR rofr = null;
    HashMap<String, FSRepository> repos = new HashMap<String, FSRepository>(8);
    HashSet<HarvestListener> listeners = new HashSet<HarvestListener>();
    Properties std = null;
    RegOrder sched = null;
    protected Logger logr = null;
    protected DocumentBuilderFactory dbfact = null;
    protected TransformerFactory transfact = null;

    static Properties defStd = null;

    final String HARVESTED_SUBDIR = "harvested";
    final String VALID_SUBDIR = "valid";
    final String INVALID_SUBDIR = "invalid";
    final String ROFR_SUBDIR = "_RofR";
    final String VAL_REPORTS_SUBDIR = "valreports";
    final String NEXTREGFILE = "_nextRegistry";
    final String VALDATE_ROOT_ELEMENT = "validate";
    final String VALDATE_RESULTS_ELEMENT = "VOResourceValidate";

    /**
     * create the repository rooted at a given directory
     * @param cache   the root directory for storing harvested records
     */
    public FullRepository(File cache) throws IOException {
        this(cache, null);
    }

    /**
     * create the repository rooted at a given directory
     * @param cache      the root directory for storing harvested records
     * @param logger     the logger to use.  If null, one will be created.
     */
    public FullRepository(File cache, Logger logger) throws IOException {
        this(cache, logger, null, null);
    }

    /**
     * create the repository rooted at a given directory
     * @param cache   the root directory for storing harvested records
     * @param logger  the logger to use.  If null, one will be created.
     * @param dbfact     an XML DocumentBuilderFactory instance to reuse; 
     *                      may be null.
     * @param transfact  an XSLT TransformerFactory instance to reuse.
     *                      may be null.
     */
    public FullRepository(File cache, Logger logger, 
                          DocumentBuilderFactory dbfact,
                          TransformerFactory transfact) 
        throws IOException 
    {
        if (dbfact == null) dbfact = DocumentBuilderFactory.newInstance();
        this.dbfact = dbfact;
        if (transfact == null) transfact = TransformerFactory.newInstance();
        this.transfact = transfact;
        logr = logger;
        if (logr == null) logr = Logger.getLogger(getClass().getName());

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
    }

    /**
     * return the Property list standard metadata
     */
    public static Properties getStdDefs() { 
        if (defStd == null) 
            defStd = RIStandard.getDefaultDefinitions();
        return defStd;
    }

    /**
     * accept a harvest listener to be attached to each harvesting process
     */
    public void addHarvestListener(HarvestListener listener) {
        if (listeners == null) listeners = new HashSet<HarvestListener>();
        listeners.add(listener);
    }

    /**
     * remove a harvest listener to attached to each harvesting process
     */
    public void removeHarvestListener(HarvestListener listener) {
        if (listeners == null) return;
        listeners.remove(listener);
    }

    /**
     * return the number publishing registries currently known to the RofR.
     * This may change after a call to 
     * {@link #updatePublishers(boolean) updatePublishers()}.
     */
    public int getPublisherCount() {
        if (rofr == null) rofr = createRofR();
        return rofr.getPublisherCount();
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
        if (rofr == null) rofr = createRofR();
        int n = rofr.updatePublishers(incremental);
        if (sched != null) sched.reload();
        return n;
    }

    /**
     * return the set of publishing registries known to the RofR
     */
    public Set<PublishingRegistry> getPublishers() {
        if (rofr == null) rofr = createRofR();
        return rofr.getPublishers();
    }

    /**
     * return the set of names for the known publishing registries.  These 
     * names do not not necessarily correspond to either the registry IDs or 
     * the shortNames; however, they are assumed to be unique.  These names 
     * are used as the basis for repository directory for harvested records.  
     */
    public Set<String> getPublisherNames() {
        if (rofr == null) rofr = createRofR();
        return rofr.getPublisherNames();
    }

    /**
     * return the PublishingRegistry summary of the registry assigned the 
     * given name. 
     */
    public PublishingRegistry getPublisher(String name) {
        if (rofr == null) rofr = createRofR();
        return rofr.getPublisher(name);
    }

    /**
     * return a RofR instance
     */
    protected RofR createRofR() {
        Logger logger = Logger.getLogger(logr.getName()+".RofR");
        File cache = new File(cacheDir, ROFR_SUBDIR);
        try {
            return new RofR(cache, false, logger, std);
        } catch (Exception ex) {
            throw new InternalError("programmer error: missing cache directory");
        }
    }

    /**
     * return the RofR instance used by this class
     */
    public RofR getRofR() {
        if (rofr == null) rofr = createRofR();
        return rofr;
    }

    /**
     * return the repository for the named registry
     * @throws IOException   if there is a failure while trying to set up the 
     *                          repository's cache
     */
    public FSRepository getRepository(String name) 
        throws IOException 
    {
        FSRepository out = repos.get(name);
        if (out == null) {
            PublishingRegistry pubreg = getPublisher(name);
            if (pubreg == null) 
                throw new IllegalArgumentException("Unrecognized registry name: "
                                                   + name);

            File repoParent = new File(cacheDir, name);
            if (! repoParent.exists() && ! repoParent.mkdir())
                throw new IOException("Failed to create repository dir for " + 
                                      name);
            File repoUse = new File(repoParent, HARVESTED_SUBDIR);

            Logger logger = Logger.getLogger(logr.getName()+".repos");
            out = new FSRepository(pubreg.getHarvestingEndpoint(), 
                                   repoUse, logger);
            out.setRegistryID(pubreg.getID());
            out.setRegistryName(name);
            for(HarvestListener listener : listeners)
                out.addHarvestListener(listener);
            repos.put(name, out);
        }
        return out;
    }
    
    /**
     * harvest from the named registry.  The name is one of those returned 
     * by {@link #getPublisherNames() getPublisherNames()}.  
     * <p>
     * Harvested records are collected into a directory named after the name
     * used to trigger the harvest (via this method).  Unvalidated records are 
     * saved to a subdirectory called "harvested"; this will be the result 
     * if <code>validate=false</code>.  If <code>validate=true</code>, the 
     * record will get moved to either the "valid" or "invalid" directory, 
     * depending on the result of validation.  If previous versions of the file
     * exist in a subdirectory, they will be replaced.  
     * @param name         the name of the publishing registry to harvest from
     * @param validate     if true, validate and segregate the incoming records.
     * @param incremental  if true, only get the new records from the last 
     *                       harvest attempt.
     */
    public int harvestFrom(String name, boolean validate, boolean incremental) 
        throws HarvestingException, IOException
    {
        FSRepository repo = getRepository(name);

        // now we are ready to harvest
        int n = repo.update(incremental);

        if (validate) {
            try {
                validateHarvested(name, repo);
            }
            catch (TestingException ex) {
                throw new IngestException("Error during validation: " + 
                                          ex.getMessage(), ex);
            }
        }

        return n;
    }

    /**
     * validate all recently harvested records segregating them into valid
     * and invalid subdirectories.  
     * @param name         the name of the publishing registry to validate 
     *                        records for.
     */
    public int validateHarvested(String name) 
        throws IOException, TestingException 
    { 
        return validateHarvested(name, null);
    }

    protected int validateHarvested(String name, FSRepository repo) 
        throws IOException, TestingException
    { 
        PublishingRegistry pubreg = getPublisher(name);
        if (pubreg == null) 
            throw new IllegalArgumentException("Unrecognized registry name: " +
                                               name);

        File reposParent = new File(cacheDir, name);
        File harvestedDir = new File(reposParent, HARVESTED_SUBDIR);
        if (! harvestedDir.exists()) {
            logr.warning(name + ": It appears validation request came before " +
                         "harvesting for the first time.");
            return 0;
        }

        if (repo == null) repo = repos.get(name);
        if (repo == null) 
            repo = new FSRepository(null, harvestedDir); // don't need url

        File validDir = new File(reposParent, VALID_SUBDIR);
        File invalidDir = new File(reposParent, INVALID_SUBDIR);
        File reportsDir = new File(reposParent, VAL_REPORTS_SUBDIR);
        if (! validDir.exists() && ! validDir.mkdir())
            throw new IOException("Failed to create "+name+File.separator+
                                  VALID_SUBDIR+" subdirectory");
        if (! invalidDir.exists() && ! invalidDir.mkdir())
            throw new IOException("Failed to create "+name+File.separator+
                                  INVALID_SUBDIR+" subdirectory");
        if (! reportsDir.exists() && ! reportsDir.mkdir())
            throw new IOException("Failed to create "+name+File.separator+
                                  VAL_REPORTS_SUBDIR+" subdirectory");

        // logr.warning("validation not yet supported");

        int nt = 0, deleted=0, nval=0, ninval=0;
        try {
            // create the tools for doing the validation
            DocumentBuilder db = dbfact.newDocumentBuilder();
            VOResourceValidater validater = createValidater(repo);
            validater.setResponseRootName(VALDATE_RESULTS_ELEMENT);
            validater.setResultTypes(ResultTypes.ADVICE);
            TransformerFactory tFactory = TransformerFactory.newInstance();
            tFactory.setAttribute("indent-number", 2);
            Transformer xmlwriter = tFactory.newTransformer();
            xmlwriter.setOutputProperty(OutputKeys.INDENT, "yes");
            xmlwriter.
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                                  "2");
            Assessor assessor = new Assessor(ResultTypes.ADVICE);

            String valrepname = null;
            File report = null, dest = null;
            FileReader rf = null;
            Document out = null;
            Element root = null;
            DOMSource source = null;
            FileWriter outf = null;
            StreamResult result = null;
            Assessment[] status = null;

            File[] resfiles = repo.resourceFiles();
            if (logr.isLoggable(Level.FINE))
                logr.fine("validating "+resfiles.length+" harvested records...");
            for(File vores : resfiles) {
                // create output file
                if (vores.getName().startsWith("res"))
                    valrepname = "val"+vores.getName().substring(3);
                else 
                    valrepname = "val-"+vores.getName();
                report = new File(reportsDir, valrepname);

                // input
                rf = new FileReader(vores);  

                // output document
                out = db.newDocument();
                out.setXmlStandalone(true);
                out.setXmlVersion("1.0");
                root = out.createElement(VALDATE_ROOT_ELEMENT);
                out.appendChild(root);

                // do the validation
                nt = validater.validate(rf, root);

                // assess the results
                status = assessor.assess(root, "VOResourceValidate", true);
                if ("deleted".equals(status[0].getStatus())) ++deleted;

                // send the output document to an XML file
                source = new DOMSource(out);
                outf = new FileWriter(report);
                result = new StreamResult(outf);
                xmlwriter.transform(source, result); 
                outf.write('\n');
                outf.close();

                // now move XML file according to the results.  
                if (status[0].failCount() > 0) {
                    dest = new File(invalidDir, vores.getName());
                    ninval++;
                }
                else {
                    dest = new File(validDir, vores.getName());
                    nval++;
                }

                if (! vores.renameTo(dest)) 
                    throw new IOException("Failed to move " +
                                          harvestedDir.getName() +File.separator+
                                          vores.getName()+ " to " +
                                          dest.getParentFile().getName() + 
                                          File.separator+dest.getName());
            }   
        }
        catch (ParserConfigurationException ex) {
            throw new InternalError("XML Config problem: "+ex.getMessage());
        }
        catch (DOMException ex) {
            throw new InternalError("DOM construction error: "+ex.getMessage());
        }
        catch (TransformerException ex) {
            throw new IOException("XML output error: "+ex.getMessage(), ex);
        }

        if (logr.isLoggable(Level.INFO))
            logr.info("found "+nval+" valid, "+ninval+" invalid records ("+
                      deleted+" deleted)");

        return 0;
    }

    protected VOResourceValidater createValidater(FSRepository repo) {
        return new VOResourceValidater(transfact);
    }

    class Assessor {
        int types = ResultTypes.ADVICE;
        final String[] statustypes = { "fail", "warn", "rec", "pass" };
        public Assessor(int types) {
            this.types = types;
        }
        Assessment createAssessment(Element forel) {
            Assessment out = new Assessment(forel);
            if ((types&ResultTypes.FAIL) > 0) out.ntype[0] = 0;
            if ((types&ResultTypes.WARN) > 0) out.ntype[1] = 0;
            if ((types&ResultTypes.REC) > 0)  out.ntype[2] = 0;
            if ((types&ResultTypes.PASS) > 0) out.ntype[3] = 0;
            return out;
        }
        public Assessment[] assess(Element root, String elname, 
                                   boolean annotate) 
        {
            Element test = null;
            Node c = null;
            String stat = null;
            Assessment out = null;
            Vector<Assessment> a = new Vector<Assessment>();
            Node child = root.getFirstChild();
            while (child != null) {
                if (child.getNodeType() == child.ELEMENT_NODE &&
                    elname.equals(child.getLocalName())) 
                {
                    out = createAssessment((Element) child);
                    c = child.getFirstChild();
                    while (c != null) {
                        if (c.getNodeType() == child.ELEMENT_NODE &&
                            ("test".equals(c.getLocalName()) ||
                             "test".equals(c.getNodeName())    )) 
                        {
                            test = (Element) c;
                            for(int i=0; i < statustypes.length; ++i) {
                                if (statustypes[i].equals(
                                      test.getAttribute("status"))) {
                                    if (out.ntype[i] < 0) out.ntype[i] = 0;
                                    out.ntype[i]++;
                                    break;
                                }
                            }
                        }
                        c = c.getNextSibling();
                    }
                    a.add(out);

                    if (annotate) {
                        if (out.failCount() >= 0) 
                            ((Element) child).setAttribute("nfail", 
                                             Integer.toString(out.failCount()));
                        if (out.warnCount() >= 0) 
                            ((Element) child).setAttribute("nwarn", 
                                             Integer.toString(out.warnCount()));
                        if (out.recCount() >= 0) 
                            ((Element)  child).setAttribute("nrec", 
                                             Integer.toString(out.recCount()));
                        if (out.testCount() >= 0) 
                            ((Element)  child).setAttribute("ntest", 
                                             Integer.toString(out.testCount()));
                    }
                }
                child = child.getNextSibling();
            }
            return a.toArray(new Assessment[a.size()]);
        }
    }

    class Assessment {
        int ncount=-1;
        int[] ntype = { -1, -1, -1, -1 };
        Element testRoot = null;

        public Assessment(Element root) { testRoot = root; }
        public int failCount() {  return ntype[0];  }
        public int warnCount() {  return ntype[1];  }
        public int recCount()  {  return ntype[2];  }
        public int passCount() {  return ntype[3];  }
        public int testCount() {  return ncount;    }
        public String getStatus() { return testRoot.getAttribute("status"); }
    }


    // this little gizmo will allow us to keep rotating the order of the
    // registries when harvesting.  
    class RegOrder extends LinkedList<String> {
        File saved = null;
        public RegOrder() throws IOException {
            this(new File(cacheDir, NEXTREGFILE));
        }
        public RegOrder(File persisted) throws IOException {
            super();
            saved = persisted;
            reload();
        }

        public void reload() throws IOException { load(saved); }
        void load(File persisted) throws IOException {
            if (persisted.exists()) {
                BufferedReader rdr = 
                    new BufferedReader(new FileReader(persisted));
                String line = null;
                while ((line = rdr.readLine()) != null) 
                    add(line.trim());
                rdr.close();
            }

            // now push on any new registries
            Set<String> regs = getPublisherNames();
            for(String name : this) 
                regs.remove(name);
            for(String name : regs) 
                addFirst(name);
            if (regs.size() > 0) save(saved);
        }

        public void save() throws IOException { save(saved); }
        void save(File persisted) throws IOException {
            PrintWriter wrtr = new PrintWriter(new FileWriter(persisted));
            for(String name : this) 
                wrtr.println(name);
            wrtr.close();
        }

        void shift() throws IOException { 
            addLast(poll()); 
            save();
        }

        boolean promote(String name) {
            if (! remove(name)) return false;
            addFirst(name);
            return true;
        }
    }

    void updateSched() throws IOException {
        if (sched == null) 
            sched = new RegOrder();
        else 
            sched.reload();
    }

    /**
     * harvest from the next one or more registries.  This class keeps an
     * ordered list of the registries in need of harvesting, starting with 
     * the registry that has gone the longest without an update.  This
     * method will step through that list (updating it as progress is made).  
     * <p>
     * This method distinguishes between local system errors (thrown as 
     * IOExceptions) and those related to a particular registry service (thrown
     * as HarvestingExceptions).  The latter will not interrupt harvesting; 
     * rather, the error will be logged and harvesting will proceed to the 
     * next registry in line.  
     * @param lim       the maximum number of registries to harvest from.  If 
     *                    if the value is less than zero or greater than the 
     *                    number of known registries,  all registries will be 
     *                    harvested from.
     * @param incremental  if true, only get the new records from the last 
     *                       harvest attempt.
     * @param validate     if true, validate and segregate the incoming records.
     * @return int   the number of new records updated.
     * @throws IOException  if an unexpected local error occurred while 
     *                       processing the harvested records.  Such an error
     *                       should be one where it would be prudent to stop
     *                       all harvesting (e.g. a full disk).  
     */ 
    public int harvestNext(int lim, boolean validate, boolean incremental)
        throws IOException
    {
        try {
            return harvestNext(lim, validate, incremental, false);
        } catch (HarvestingException ex) {
            // should not happen
            throw new InternalError("programmer error: unexpected harvesting " +
                                    "error");
        }
    }

    /**
     * harvest incrementally from the next one or more registries.  This class 
     * keeps anordered list of the registries in need of harvesting, starting 
     * with the registry that has gone the longest without an update.  This
     * method will step through that list (updating it as progress is made).  
     * <p>
     * This method distinguishes between local system errors (thrown as 
     * IOExceptions) and those related to a particular registry service (thrown
     * as HarvestingExceptions).  The latter will not interrupt harvesting; 
     * rather, the error will be logged and harvesting will proceed to the 
     * next registry in line.  
     * @param lim       the maximum number of registries to harvest from.  If 
     *                    if the value is less than zero or greater than the 
     *                    number of known registries,  all registries will be 
     *                    harvested from.
     * @param validate     if true, validate and segregate the incoming records.
     * @return int   the number of new records updated.
     * @throws IOException  if an unexpected local error occurred while 
     *                       processing the harvested records.  Such an error
     *                       should be one where it would be prudent to stop
     *                       all harvesting (e.g. a full disk).  
     */ 
    public int harvestNext(int lim, boolean validate)
        throws IOException
    {
        return harvestNext(lim, validate, true);
    }

    /**
     * harvest from the next one or more registries.  This class keeps an
     * ordered list of the registries in need of harvesting, starting with 
     * the registry that has gone the longest without an update.  This
     * method will step through that list (updating it as progress is made).  
     * <p>
     * This method distinguishes between local system errors (thrown as 
     * IOExceptions) and those related to a particular registry service (thrown
     * as HarvestingExceptions).  Unless haltOnError=true, the latter will
     * not interrupt harvesting; rather, the error will be logged and harvesting
     * will proceed to the next registry in line.  
     * @param lim       the maximum number of registries to harvest from.  If 
     *                    if the value is less than zero or greater than the 
     *                    number of known registries,  all registries will be 
     *                    harvested from.
     * @param incremental  if true, only get the new records from the last 
     *                       harvest attempt.
     * @param validate     if true, validate and segregate the incoming records.
     * @param haltOnError  if true, any failures that appear to be specific to 
     *                       a particular service will halt all further 
     *                       harvesting; otherwise, such a failure will be 
     *                       logged and harvesting will commence on the next
     *                       registry.
     * @return int   the number of new records updated.
     * @throws IOException  if an unexpected local error occurred while 
     *                       processing the harvested records.  Such an error
     *                       should be one where it would be prudent to stop
     *                       all harvesting (e.g. a full disk).  
     * @throws HarvestingException  if a service-specific error occurs, thrown 
     *                       only if haltOnError=true, 
     */ 
    public int harvestNext(int lim, boolean validate, boolean incremental, 
                           boolean haltOnError) 
        throws HarvestingException, IOException
    {
        if (lim == 0) return 0;
        if (sched == null) sched = new RegOrder();
        if (lim < 0 || lim >= sched.size()) lim = sched.size();

        int regcnt = 0, reccnt = 0, n = 0;
        String reg = null;
        for(regcnt=0; regcnt < lim; ++regcnt) {
            reg = sched.peek();
            n = 0;
            try {
                if (logr.isLoggable(Level.FINE))
                    logr.info("harvesting from " + reg + "...");
                sched.shift();
                n = harvestFrom(reg, validate, incremental);
            }
            catch (HarvestingException ex) {
                logr.severe("Error encountered while harvesting from " + reg +
                            ": " + ex.getMessage());
                n = ex.getCompletedRecordCount();
                if (n < 0) n = 0;
                if (haltOnError) throw ex;
            }
            finally {
                reccnt += n;
                logr.info("New records harvested from " + reg + ": " + n);
            }
        }
        return reccnt;
    }

    /**
     * do a complete, incremental harvesting of the VO starting with a 
     * search of the RofR for new registries.  
     * @param validate     if true, validate and segregate the incoming records.
     * @param lim       the maximum number of registries to harvest from.  If 
     *                    if the value is less than zero or greater than the 
     *                    number of known registries,  all registries will be 
     *                    harvested from.
     * @param haltOnError  if true, any failures that appear to be specific to 
     *                       a particular service will halt all further 
     *                       harvesting; otherwise, such a failure will be 
     *                       logged and harvesting will commence on the next
     *                       registry.
     * @throws IOException  if an unexpected local error occurred while 
     *                       processing the harvested records.  Such an error
     *                       should be one where it would be prudent to stop
     *                       all harvesting (e.g. a full disk).  
     * @throws HarvestingException  if (when haltOnError=true) a 
     *                       service-specific error occurs, OR a harvesting 
     *                       error occurs while consulting the RofR.
     */
    public int update(boolean validate, int lim, boolean haltOnError) 
        throws HarvestingException, IOException
    {
        updatePublishers(true);
        return harvestNext(lim, validate, true, haltOnError);
    }
     
    /**
     * do a complete, incremental harvesting of the VO starting with a 
     * search of the RofR for new registries.  
     * @param validate     if true, validate and segregate the incoming records.
     * @param lim       the maximum number of registries to harvest from.  If 
     *                    if the value is less than zero or greater than the 
     *                    number of known registries,  all registries will be 
     *                    harvested from.
     * @throws IOException  if an unexpected local error occurred while 
     *                       processing the harvested records.  Such an error
     *                       should be one where it would be prudent to stop
     *                       all harvesting (e.g. a full disk).  
     * @throws HarvestingException  if a harvesting 
     *                       error occurs while consulting the RofR.
     */
    public int update(boolean validate, int lim) 
        throws HarvestingException, IOException
    {
        return update(validate, lim, false);
    }

    /**
     * do a complete, incremental harvesting of all known registries starting 
     * with a search of the RofR for new registries.  
     * @param validate     if true, validate and segregate the incoming records.
     * @throws IOException  if an unexpected local error occurred while 
     *                       processing the harvested records.  Such an error
     *                       should be one where it would be prudent to stop
     *                       all harvesting (e.g. a full disk).  
     * @throws HarvestingException  if a harvesting 
     *                       error occurs while consulting the RofR.
     */
    public int update(boolean validate) 
        throws HarvestingException, IOException
    {
        return update(validate, -1);
    }

    public static void main(String[] args) {
        if (args.length < 1) 
            throw new IllegalArgumentException("missing repos directory name");
        String dir = args[0];
        int i = -1;
        if (args.length > 1) i = Integer.parseInt(args[1]);

        try {
            FullRepository.configLogger();
            Logger logger = Logger.getLogger("Repository");
            logger.setLevel(Level.FINE);
            FullRepository repo = new FullRepository(new File(dir), logger);

            int n = repo.updatePublishers(true);
            System.out.println("Found " + n + " new registries");

            repo.updateSched();
            for(int j=args.length-1; j > 1; --j)
                repo.sched.promote(args[j]);
            System.out.println("Harvest Order:");
            for(String name : repo.sched) 
                System.out.println("  "+name);

            if (i >= 0) {
                logger.info("Starting harvest on next " + i + " registries...");
                repo.addHarvestListener(new ProgBar(logger));
                repo.harvestNext(i, true);
            }
        }
        catch (IllegalArgumentException ex) {
            System.err.println("FullRepo: " + ex.getMessage());
            System.err.println("Usage: FullRepo dir");
            System.exit(1);
        }
        catch (HarvestingException ex) {
            System.err.println("FullRepo: " + ex.getMessage());
            System.err.println("URL: " + ex.getRequestURL());
            System.exit(2);
        }
        catch (Exception ex) {
            System.err.println("FullRepo: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(2);
        }

    }

    static void configLogger() throws IOException {
        Logger root = Logger.getLogger("");
        for(Handler h : root.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                h.setLevel(Level.FINE);
                h.setFormatter(new TerseFormatter());
                break;
            }
        }
    }
}

class ProgBar implements HarvestListener {
    Logger logr = null;
    ProgBar(Logger logger) { logr = logger; }
    public void tellWantedItems(Set<String> names) {
        names.add(REQUEST_URL);
    }
    public void harvestInfo(int page, String id,String itemName,String value) {
        if (REGISTRY_NAME.equals(itemName)) {
            if (logr == null || ! logr.isLoggable(Level.FINE)) 
                System.out.print("Harvesting from " + value + "...");
        }
        else if (REQUEST_URL.equals(itemName)) {
            if (logr != null && logr.isLoggable(Level.FINE)) 
                logr.fine("Using URL: " + value);
            else
                System.out.print(".");
        }
        else if (HARVEST_COMPLETE.equals(itemName)) 
            System.out.println(".");
    }
}

class TerseFormatter extends Formatter {
    public TerseFormatter() { }
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getLoggerName()).append(' ');
        sb.append(record.getLevel().getLocalizedName()).append(": ");
        sb.append(formatMessage(record));
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }
        sb.append("\n");
        return sb.toString();
    }
}