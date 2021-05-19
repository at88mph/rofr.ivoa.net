package net.ivoa.registry.validate;

import org.nvo.service.validation.ParsingErrors;
import org.nvo.service.validation.HTTPGetTestQuery;
import org.nvo.service.validation.ResultTypes;
import org.nvo.service.validation.TestQuery;
import org.nvo.service.validation.TemplateTestQuery;
import org.nvo.service.validation.TestingException;
import org.nvo.service.validation.ProcessingException;
import org.nvo.service.validation.ValidaterListener;
import org.nvo.service.validation.StatusHelper;
import org.nvo.service.validation.XSLEvaluator;
import org.nvo.service.validation.CachingTester;
import org.nvo.service.validation.ConfigurationException;
import org.nvo.service.validation.TimeoutException;
import org.nvo.service.validation.UnrecognizedResponseTypeException;
import org.nvo.service.validation.ProcessingException;
import net.ivoa.registry.harvest.iterator.DocumentIterator;
import net.ivoa.registry.harvest.iterator.HarvestRecordServer;
import net.ivoa.registry.harvest.iterator.VOResourceExtractor;
import net.ivoa.registry.harvest.iterator.VOResourceCache;
import net.ivoa.registry.harvest.Harvester;
import net.ivoa.registry.harvest.HarvestingException;
import ncsa.xml.validation.SchemaLocation;
import ncsa.xml.validation.ValidationUtils;
import net.ivoa.util.Configuration;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.parsers.ParserConfigurationException; 
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

/**
 * a validater for the Harvesting interface of an IVOA-compliant registry 
 * that encodes the results in an XML format.  Unlike other validater classes
 * in this library, this one is configured on a per-endpoint basis; this allows 
 * it to make use of an optional disk cache for building up the XML results.
 * 
 */
public class HarvestValidater {

    protected Configuration config = null;
    protected TransformerFactory tfact = TransformerFactory.newInstance();
    protected DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
    protected static StatusHelper statushelper = new StatusHelper();
    protected boolean verbose = false;
    protected String resultRootName = null;
    protected String queryRootName = null;
    protected ResultTypes defResultTypes = new ResultTypes(ResultTypes.ADVICE);
    protected LinkedList queries = new LinkedList();
    protected Properties evalprops = new Properties();
    protected SchemaLocation sl = null;
    protected int notGoodEnough = ResultTypes.FAIL;

    protected String baseurl = null;
    protected File cacheDir = null;

    // locks to allow multiple requests for results but only one validation
    protected Object oailock = new Object();
    protected Object ivoalock = new Object();
    protected Object vorlock = new Object();

    private HashSet validatingThreads = new HashSet();
    private boolean[] nowValidating = { false, false, false };
    protected final static int OAI  = 0;
    protected final static int IVOA = 1;
    protected final static int VOR  = 2;

    protected Properties names = null;
    protected boolean builtinSchemas = false;


    /**
     * create a HarvestValidater 
     * @param conf      the Configuration for this validater.  The base URL
     *                     must be supplied therein lest this constructor
     *                     will fail. 
     */
    public HarvestValidater(Configuration conf) 
        throws ConfigurationException
    {
        this(conf, null);
    }

    /**
     * create a HarvestValidater 
     * @param conf      the Configuration for this validater
     * @param baseURL   the harvesting endpoint of the registry to test
     */
    public HarvestValidater(Configuration conf, String baseURL) 
        throws ConfigurationException
    {
        config = conf;
        if (config == null) 
            throw new NullPointerException("missing Configuration for " + 
                                           "HarvestValidater");
        baseurl = baseURL;
        if (baseurl == null) baseurl = config.getParameter("testQuery/baseURL");
        if (baseurl == null) 
            throw new NullPointerException("missing base URL for " + 
                                           "HarvestValidater");

        names = loadNames(config);
        resultRootName = nm("RIValRootElem");
        queryRootName = nm("testQueryElem");

        sl = new SchemaLocation(getClass());

        // load up the configured queries that test the IVOA profile on OAI.
        Configuration tqconfig = 
            HTTPGetTestQuery.getTestQueryConfig(config, "httpget");
        if (tqconfig != null)
            HTTPGetTestQuery.addAllQueryData(tqconfig, queries);
        if (queries.size() <= 0) 
            throw new ConfigurationException("no test queries configured");
    }

    /**
     * create a HarvestValidater 
     * @param conf            the Configuration for this validater
     * @param baseURL         the harvesting endpoint of the registry to test
     * @param cacheDir        a directory where results can be cached.  This will 
     *                          override the directory specified in the configuration.
     * @param builtinSchemas  if true, do not use xsd's in cache
     */
    public HarvestValidater(Configuration conf, String baseURL, File cacheDir, boolean builtinSchemas) 
        throws ConfigurationException, FileNotFoundException
    {
        this(conf, baseURL);

        if (cacheDir != null) {
            setCacheDir(cacheDir, false);
        }
        else {
            String dir = config.getParameter("cacheHome");
            if (dir != null) setCacheDir(new File(dir), true);
        }
        this.builtinSchemas = builtinSchemas;

    }

    /**
     * set the cache directory that should be used for validation.  If 
     * another directory was already set, any results in that directory will
     * be forgotten.  
     * @param dir        the directory to use for caching files.  
     * @param useSubdir  if true, the given directory is a parent directory
     *                      under which a new directory will be created with 
     *                      a name of this class's choosing to use as a cache.  
     *                      If false, the given directory will be used 
     *                      directly; no new directory will be created. 
     * @exception FileNotFoundException   if the parent directory does not 
     *                      exist.
     */
    public void setCacheDir(File dir, boolean useSubdir) 
        throws FileNotFoundException
    {
        File parent = dir;
        if (useSubdir) {
            Pattern p1 = Pattern.compile("^\\w+://");
            Pattern p2 = Pattern.compile("/");
            String subdir = 
               p2.matcher(p1.matcher(baseurl).replaceFirst("")).replaceAll("_");
            dir = new File(parent, subdir);
        }
        else {
            parent = dir.getParentFile();
        }

        if (dir.exists()) {
            if (! dir.isDirectory())
                throw new IllegalArgumentException("Not a directory: " + dir);
        }
        else {
            if (! parent.exists())
                throw new FileNotFoundException("Cache parent directory does " +
                                                "not exist: " + parent);
            if (! parent.isDirectory()) 
                throw new IllegalArgumentException("Not a directory: " + parent);
        }

        cacheDir = dir;
    }

    /**
     * return the actual directory that will be used to contain the cached
     * files.  Null is returned if no directory has been set, yet.  Validation
     * will still work without one, but may be slow or ineffiecient.  
     */
    public File getCacheDir() { return cacheDir; }

    /**
     * return true if all exceptions will be reported via standard error.
     * The default is false.
     */
    public boolean isVerbose() { return verbose; }

    /**
     * set whether all exceptions will be reported via standard error.
     * @param yes     true, if all exceptions should be reported
     */
    public void setVerbose(boolean yes) { verbose = yes; }

    /**
     * include particular types of test result (FAIL, WARN, REC, or PASS, 
     * usually) by default in the validation results.  The configuration file
     * may override this.  
     * @param type   the type to include OR-ed together.  These are usually 
     *                 taken from the defined constants from 
     *                 {@link org.nvo.service.validatoin.ResultType ResultType},
     *                 but it can cover user-defined values.
     */
    public void addDefaultResultTypes(int type) {
        defResultTypes.addTypes(type);
    }

    /**
     * set the  particular types of test result (FAIL, WARN, REC, or PASS, 
     * usually) by default in the validation results.  The configuration file
     * may override this.  
     * @param type   the types to include OR-ed together.  These are usually 
     *                 one of the defined constants from 
     *                 {@link org.nvo.service.validatoin.ResultType ResultType},
     *                 but it can cover user-defined values.
     */
    public void setDefaultResultTypes(int type) {
        defResultTypes.setTypes(type);
    }

    /**
     * register the current thread as in the process of validating.  This 
     * should be called at the start of the execution of a validating method 
     * so that it can be cleanly interrupted via this class's 
     * {@link #interrupt() interrupt()} method.  
     * @param which   the type of validating going on, represented by one 
     *                   of the constants, OAI, IVOA, or VOR.
     */
    protected synchronized void validating(int which) {
        nowValidating[which] = true;
        validatingThreads.add(Thread.currentThread());
    }

    /**
     * unregister the current thread as in the process of validating.  This 
     * should be called at the end of a validating method via a finally block
     * @param which   the type of validating going on, represented by one 
     *                   of the constants, OAI, IVOA, or VOR.
     */
    protected synchronized void doneValidating(int which) {
        nowValidating[which] = false;
        validatingThreads.remove(Thread.currentThread());
    }

    /**
     * signal all threads currently running validate() to interrupt their 
     * testing and exit as soon as possible.  
     */
    public final synchronized void interrupt() { 
        Iterator it = validatingThreads.iterator();
        while (it.hasNext()) {
            ((Thread) it.next()).interrupt();
        }
    }

    /**
     * return true if there is at least one Thread executing one of the 
     * validate methods.
     */
    public boolean isValidating() {  return (validatingThreads.size() > 0); }

    /**
     * return true if there is at least one Thread executing a particular
     * validate method.
     * @param which   the type of validating on interest, represented by one 
     *                   of the constants, OAI, IVOA, or VOR.
     */
    public boolean isValidating(int which) {  return nowValidating[which]; }

    /**
     * wait for all validating threads to complete their validation.  That is,
     * wait up to the given amount of time for the {@link #isValidating()} 
     * method to return false.
     * @param millis   the amount of time to wait.  If <= 0, wait indefinitely.
     */
    public void waitForValidation(long millis) throws InterruptedException {
        long start = (new Date()).getTime();
        long now = start;
        long step = 100;
        while (! isValidating() && (millis <= 0 || now-start > millis)) {
            Thread.sleep(step);
            now = (new Date()).getTime();
            if (step < 2000) step *= 2;
        }
    }

    /**
     * return the results of validating the standard OAI-PMH interface.  If 
     * this validation has already been completed and cached, those results
     * will be returned.  If validation is already in progress, this call
     * will wait until it is complete and then return the results.  If 
     * validation has not started yet, it will initiate the validation.  
     * @param listener   a listener that will be notified of progress
     */
    public Document validateOAI(ValidaterListener listener, Map status)
         throws TestingException, IOException, InterruptedException
    {
        File explorerOut = null;
        File oairesults  = null;
        if (cacheDir != null) {
            explorerOut = new File(cacheDir, nm("ExplorerOutputFile"));
            oairesults = new File(cacheDir, nm("OAIValResultsFile"));
        }

        // setup for tracking status
        String id = null;
        if (listener != null) {
            status = setStatus(status);
            id = (String) status.get("id");
        }
        
        if (isValidating(OAI)) 
            tellProgress(listener, status, "waiting", "OAI", "OAI validation " +
                         "already in progress; waiting for completion.");

        synchronized (oailock) {
            if (oairesults != null && oairesults.exists()) {
                tellProgress(listener,status,"OAI","completed",
                             "OAI validation complete.");
                return readDocument(oairesults);
            }

            try {
                validating(OAI);
                tellProgress(listener, status, "started", "OAI",
                             "OAI validation begun.");

                if (cacheDir != null && ! cacheDir.exists()) {
                    cacheDir.mkdir();
                }

                // look a local installation of the OAI Explorer
                File explorercmd = null;
                String explorerurl = null;
                Configuration tqconfig = 
                    OAIExplorerTestQuery.getTestQueryConfig(config, null);
                explorerurl = tqconfig.getParameter("oaiExplorerURL");
                if (tqconfig != null) {
                    String val = tqconfig.getParameter("oaiExplorerCmd");
                    if (val != null) {
                        explorercmd = new File(val);
                        if (! explorercmd.exists()) {
                            complain("Warning: OAI Explorer command not found: "
                                     + val);
                            explorercmd = null;
                        }
                    }
                }
                if (explorercmd == null && explorerurl == null) 
                    throw new ConfigurationException("No usable OAIExplorer " +
                                                  "URL or local command found");

                // get the OAI TestQuery
                TestQuery tq = null;
                if (explorercmd != null) {
                    // run locally
                    tq = new OAIExplorerTestQuery(new URL(baseurl), explorercmd);
                }
                else {
                    // use the web service
                    tq = new OAIExplorerTestQuery(new URL(baseurl), explorerurl);
                }

                // create the Evaluator
                OAIEvaluator eval = new OAIEvaluator();
                eval.setQueryElemName(nm("OAIResultQueryElem"));

//                 // Cache the tool response to disk
//                 OutputStream eos = null;
//                 InputStream eis = null;
//                 try {
//                     eos = new FileOutputStream(explorerOut);
//                 } catch (IOException ex) {
//                     eos = null;
//                 }

//                 if (eos != null) {
//                     passThru(eval.getStream(tq), eos);
//                     eis = new FileInputStream(explorerOut);
//                 }
//                 else {
//                     eis = eval.getStream(tq);
//                 }

                // create the output document and apply the OAI tests, 
                // caching the Explorer response along the way
                Document results = df.newDocumentBuilder().newDocument();
                Element root = results.createElement(nm("OAIResultRootElem"));
                root.setAttribute("baseURL", baseurl);
                root.appendChild(results.createTextNode("\n"));
                results.appendChild(root);

                if (Thread.interrupted()) throw new 
                    InterruptedException("Validation shutdown requested");

                CachingTester tester = new CachingTester(tq, eval, explorerOut);
                int i = tester.applyTests(root);
                root.setAttribute("testCount", Integer.toString(i));
                root.setAttribute("showStatus", tq.getResultTokens());

                // cache the XML-formatted results to disk
                if (oairesults != null) {
                    OutputStream eos = new FileOutputStream(oairesults);
                    Transformer printer = tfact.newTransformer();
                    printer.transform(new DOMSource(results), 
                                      new StreamResult(eos));
                    eos.close();
                }

                return results;
            }
            catch (DOMException ex) {
                throw new ProcessingException(ex);
            }
            catch (ParserConfigurationException ex) {
                throw new ProcessingException(ex);
            }
            catch (TransformerException ex) {
                throw new ProcessingException(ex);
            }
            finally {
                doneValidating(OAI);
                tellProgress(listener, status, "completed", "OAI",
                             "OAI validation complete.");
            }
        }
    }

    private Document readDocument(File docfile) throws ProcessingException {
        try {
            DocumentBuilder db = df.newDocumentBuilder();
            return db.parse(new FileInputStream(docfile));
        }
        catch (SAXException ex) {
            throw new ProcessingException(ex);
        }
        catch (IOException ex) {
            throw new ProcessingException(ex);
        }
        catch (ParserConfigurationException ex) {
            throw new ProcessingException(ex);
        }
    }

    protected void passThru(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[16*1024];
        int n = 0;
        while ((n = is.read(buffer)) >= 0) {
            os.write(buffer, 0, n);
        }
    }

    /**
     * return the results of validating the OAI interface according to IVOA
     * harvesting standards.  If this validation has already been completed 
     * and cached, those results will be returned.  If validation is already 
     * in progress, this call will wait until it is complete and then return 
     * the results.  If validation has not started yet, it will initiate the 
     * validation.  
     * @param listener   a listener that will be notified of progress
     */
    public Document validateIVOAHarvest(ValidaterListener listener, Map status) 
        throws TestingException, IOException 
    {
        int count = 0, ntests = 0;
        String listRecPrefix = nm("HarvestResponseBase");
        File resultsfile = null;
        if (cacheDir != null) {
            resultsfile = new File(cacheDir, nm("HarvestValResultsFile"));
        }

        String id = null;
        if (listener != null) {
            status = setIVOAStatus(status);
            id = (String) status.get("id");
        }

        if (isValidating(IVOA)) 
            tellProgress(listener, status, "waiting", "IVOA", "IVOA Harvest " + 
                         "validation in progress; waiting for completion");

        // make other callers wait until we're done
        synchronized (ivoalock) {

            // if we have cached results, just return them
            if (resultsfile != null && resultsfile.exists()) {
                tellProgress(listener, status, "completed", "IVOA",
                             "IVOA harvest validation complete.");
                return readDocument(resultsfile);
            }

            try {
                validating(IVOA);
                tellProgress(listener, status, "started", "IVOA",
                             "IVOA harvest validation begun.");

                if (cacheDir != null && ! cacheDir.exists()) {
                    cacheDir.mkdir();
                }

                OAIParsingErrors pe = new OAIParsingErrors();
                Configuration econfig = 
                    config.getConfiguration("evaluator", "name", "ivoa");
                XSLEvaluator eval = new XSLEvaluator(econfig, tfact, getClass());
                eval.setResponseRootName(nm("testQueryElem"));
                eval.setParsingErrorHandler(pe);
                setForXMLValidation(eval.getDocumentBuilderFactory());

                if (listener != null) 
                    status.put("totalQueryCount", new Integer(queries.size()));

                // create a TestQuery template
                HTTPGetTestQuery tq = new HTTPGetTestQuery(baseurl+'?', 
                                                           evalprops);
                tq.setResultTypes(defResultTypes.getTypes());
                tq.setEvalProperty("rightnow", rightNow());
                ListIterator it = queries.listIterator();

                if (! it.hasNext()) 
                    throw new ConfigurationException("no queries configured");

                // create the output XML document
                Document resdoc = df.newDocumentBuilder().newDocument();
                Element results = resdoc.createElement(nm("HarvestResultElem"));
                results.setAttribute("baseURL", baseurl);
                results.setAttribute("showStatus", tq.getResultTokens());
                results.appendChild(resdoc.createTextNode("\n"));
                resdoc.appendChild(results);
            
                // apply tests
                File queryresp = null;
                CachingTester tester = null;
                int i = 0;
                while (it.hasNext()) {
                    tq.setQueryData((HTTPGetTestQuery.QueryData) it.next());
                    pe.setIncludePass((tq.getResultTypes()&ResultTypes.PASS)>0);

                    if (listener != null) 
                        tellIVOAProgress(listener, status, "started", 
                                         tq.getName(), tq.getDescription());

                    if (cacheDir != null) {
                        queryresp = 
                            new File(cacheDir, listRecPrefix + (++i) + "-" +
                                     tq.getName() + ".xml");
                    }

                    tester = new CachingTester(tq, eval, queryresp);
                    try {
                        if (status != null) status.remove("exception");

                        if (Thread.interrupted()) throw new 
                           InterruptedException("Validation shutdown requested");

                        ntests = tester.applyTests(results);
                        count += ntests;
                        if (status != null) {
                            status.put("ok", Boolean.TRUE);
                            status.put("queryTestCount", new Integer(ntests));
                            status.put("totalTestCount", new Integer(count));
                        }
                    }
                    catch (ConfigurationException ex) {
                        String message = "Internal Validater Error: "+ 
                            ex.getMessage();
                        complain(message);
                        ex.printStackTrace();

                        message += 
                            " (Please report to validater service provider.)";
                        if (status != null) {
                            status.put("ok", Boolean.FALSE);
                            status.put("message", message);
                            status.put("exception", ex);
                        }
                        addTestingFailure(results, tq, "validater", message);
                    }
                    catch (TimeoutException ex) {
                        String message = "No response from registry (" +
                            ex.getMessage() + ")";
                        if (verbose) complain(message);
                        if (status != null) {
                            status.put("ok", Boolean.FALSE);
                            status.put("message", message);
                        }
                        addTestingFailure(results, tq, "timeout", message);
                        // break;
                    }
                    catch (TestingException ex) {
                        String message = addCommFailure(results, tq, ex);
                        if (verbose) complain(message);
                        if (status != null) {
                            status.put("ok", Boolean.FALSE);
                            status.put("message", message);
                        }
                    }
                    catch (InterruptedException ex) {
                        String message = 
                            "Testing interrupted while waiting for response";
                        if (verbose) complain(message);
                        if (status != null) {
                            status.put("ok", Boolean.FALSE);
                            status.put("message", message);
                        }
                        addTestingFailure(results, tq, "interrupted", message);

                        // this will signal the caller to clean up
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // pull out some info needed for upcoming tests
                    Element tqresults = eval.getLastChildElement(results);
                    String attval = tqresults.getAttribute("regid");
                    if (attval != null && attval.length() > 0) 
                        tq.setEvalProperty("registryID", attval.trim());

                    attval = tqresults.getAttribute("manAuthIDs");
                    if (attval != null && attval.length() > 0) 
                        tq.setEvalProperty("managedAuthorityIDs", attval.trim());

                    if (tq.getName().equals("ListRecords")) {
                        // note that we have seen the Registry record within 
                        // ListRecords
                        attval = tqresults.getAttribute("seenRegistryRec");
                        if (attval != null && attval.length() > 0) 
                            tq.setEvalProperty("seenRegistryRecord", 
                                               attval.trim());

                        // save the authority IDs we have encountered so far ...
                        attval = tqresults.getAttribute("foundAuthIDs");
                        if (attval != null && attval.length() > 0) 
                            tq.setEvalProperty("foundAuthorityIDs", 
                                               attval.trim());

                        // save the authority IDs that have been declared ...
                        attval = tqresults.getAttribute("declAuthIDs");
                        if (attval != null && attval.length() > 0) 
                            tq.setEvalProperty("declaredAuthorityIDs", 
                                               attval.trim());

                        attval = tqresults.getAttribute("resumptionToken");
                        if (attval != null && attval.length() > 0) {
                            // we need to get more records; slip in another test
                            HTTPGetTestQuery.QueryData resume = 
                                new HTTPGetTestQuery.QueryData("ListRecords",
                                   tq.getType(),
                                   "verb=ListRecords&resumptionToken="+attval, 
                                   "continuing results from previous query");
                            it.add(resume);
                            it.previous();
                        }
                    }

                    if (listener != null) 
                        tellIVOAProgress(listener, status, "completed", 
                                         tq.getName(), tq.getDescription());
                }

                // cache the XML-formatted results to disk
                if (resultsfile != null) {
                    OutputStream os = new FileOutputStream(resultsfile);
                    Transformer printer = tfact.newTransformer();
                    printer.transform(new DOMSource(results), 
                                      new StreamResult(os));
                    os.close();
                }

                return resdoc;
            }
            catch (DOMException ex) {
                throw new ProcessingException(ex);
            }
            catch (ParserConfigurationException ex) {
                throw new ProcessingException(ex);
            }
            catch (TransformerException ex) {
                throw new ProcessingException(ex);
            }
            finally {
                doneValidating(IVOA);
                tellProgress(listener, status, "completed", "IVOA",
                             "completed IVOA harvesting queries.");
            }
        }
    }

    private String rightNow() {
        Date now = new Date();
        TimeZone tz = TimeZone.getDefault();
        now.setTime(now.getTime() - tz.getOffset(now.getTime()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return sdf.format(now);
    }

    private void complain(String message) {
        String date = DateFormat.getDateInstance().format(new Date());
        System.err.println(date + ": baseURL=" + baseurl + "\n  " + message);
    }

    /**
     * return the results of validating the individual VOResource records
     * coming from the ListRecords call.
     * If this validation has already been completed and cached, 
     * those results will be returned.  If validation is already in progress, 
     * this call will wait until it is complete and then return the results.  
     * If validation has not started yet, it will initiate the validation.  
     * @param listener    a listener that will be notified of progress
     * @param includeMax  include results of only up to this number of 
     *                       VOResources.  If negative, return results from all 
     *                       VOResources.  Note that all records will be checked
     *                       and their results cached regardless.  
     */
    public Document validateVOResources(ValidaterListener listener, Map status,
                                        int includeMax) 
        throws TestingException, IOException, InterruptedException
    {
        // progress, file names, statistics 

        // files and file fragments that we will employ.  Note that if 
        // cacheDir is null, then we have no disk cache and must do everything
        // in memory.
        File resultsfile = null;
        if (cacheDir != null) {
            resultsfile = new File(cacheDir, nm("VORValSummaryFile"));
        }
        File resourceDir = new File(cacheDir, nm("VORRecordDir"));
        File resultsDir  = new File(cacheDir, nm("VORResultsDir"));
        String vorbase = nm("VORRecordBase");
        
        // stats we will keep track of
        int count = 0, ntests = 0, nrecs = 0;
        if (includeMax < 0) includeMax = Integer.MAX_VALUE;

        // setup for tracking status
        String id = null;
        if (listener != null) {
            status = setStatus(status);
            id = (String) status.get("id");
        }
        
        if (isValidating(VOR)) 
            tellProgress(listener, status, "waiting", "VOR", "VOResource " +
                         "validation in progress; waiting for completion");

        synchronized (vorlock) {

            // if we have cached results, just return them
            if (resultsfile != null && resultsfile.exists()) {
                tellProgress(listener, status, "completed", "VOR",
                             "VOResource record validation complete.");
                return readDocument(resultsfile);
            }

            try {
                isValidating(VOR);
                tellProgress(listener, status, "started", "VOR",
                             "VOResource record validation begun.");

                if (cacheDir != null && ! cacheDir.exists()) {
                    cacheDir.mkdir();
                }

                // create classes we will use to process each VOResource record
                VOResourceParsingErrors pe = new VOResourceParsingErrors();
                Configuration econfig = 
                    config.getConfiguration("evaluator", "name", "voresource");
                XSLEvaluator eval = new XSLEvaluator(econfig, tfact, getClass());
                eval.setResponseRootName(nm("testQueryElem"));
                eval.setParsingErrorHandler(pe);
                TemplateTestQuery tq = 
                    new TemplateTestQuery(defResultTypes, null);
                pe.setIncludePass((tq.getResultTypes() & ResultTypes.PASS) > 0);

                // the output document.  We'll reuse the builder
                DocumentBuilder db = df.newDocumentBuilder();
                Document out = db.newDocument();
                Element sampleroot = out.createElement(nm("VORResultElem"));
                sampleroot.appendChild(out.createTextNode("\n"));
                sampleroot.setAttribute("showStatus", tq.getResultTokens());
                out.appendChild(sampleroot);

                // the transformer for printing XML to a file
                Transformer printer = tfact.newTransformer();

                if (Thread.interrupted()) throw new 
                    InterruptedException("Validation shutdown requested");

                DocumentIterator di = null;
                boolean cachevors = false;

                // gain access to the individual VOResource records either 
                // via the cache or directly from the registry.
                if (cacheDir == null) {
                    // we're not caching anything
                    HarvestRecordServer h = 
                        new HarvestRecordServer(new URL(baseurl));
                    di = h.records();
                }
                else {
                    if (! resourceDir.exists()) resourceDir.mkdir();
                    if (! resultsDir.exists()) resultsDir.mkdir();

                    // do we already have the ListRecords responses cached?
                    File[] listrecords = listListRecordsFiles(cacheDir);
                    if (listrecords == null) {
                        // harvest the VOResource records directly from the 
                        // registry
                        Harvester h = new Harvester(new URL(baseurl));
                        h.harvestToDir(resourceDir, vorbase);
                    }
                    else {
                        // extract them from the listrecords
                        extractFromListRecords(listrecords,resourceDir,vorbase);
                    }

                    VOResourceCache vorc = new VOResourceCache(resourceDir);
                    vorc.setDocumentBuilder(makeVOResourceParser(pe));
                    if (status != null) 
                        status.put("totalQueryCount", new Integer(vorc.size()));
                    di = vorc.records();
                }

                Document vor = null;
                nrecs = 0;
                while ((vor = di.nextDocument()) != null) {
                    if (status != null) status.remove("exception");

                    if (Thread.interrupted()) throw new 
                        InterruptedException("Validation shutdown requested");

                    Document recresult = db.newDocument();
                    Element root = recresult.createElement(nm("VORResultElem"));
                    root.appendChild(recresult.createTextNode("\n"));
                    root.setAttribute("showStatus", tq.getResultTokens());
                    recresult.appendChild(root);

                    try {
                        ntests = eval.applyTests(vor, tq, root);
                        count += ntests;
                    }
                    catch (ConfigurationException ex) {
                        String message = "Internal Validater Error: "+ 
                            ex.getMessage();
                        complain(message);
                        ex.printStackTrace();

                        message += 
                            " (Please report to validater service provider.)";
                        if (status != null) {
                            status.put("ok", Boolean.FALSE);
                            status.put("message", message);
                            status.put("exception", ex);
                        }
                        addTestingFailure(root, tq, "validater", message);
                    }
                    catch (TimeoutException ex) {
                        String message = "No response from registry (" +
                            ex.getMessage() + ")";
                        if (verbose) complain(message);
                        if (status != null) {
                            status.put("ok", Boolean.FALSE);
                            status.put("message", message);
                        }
                        addTestingFailure(root, tq, "timeout", message);
                        // break;
                    }
                    catch (TestingException ex) {
                        String message = addCommFailure(root, tq, ex);
                        if (verbose) complain(message);
                        if (status != null) {
                            status.put("ok", Boolean.FALSE);
                            status.put("message", message);
                        }
                    }

                    Element te = eval.getLastChildElement(root);
                    pe.insertErrors(te, te.getFirstChild(),
                                    recresult.createTextNode("\n    "));
                    pe.clear();
                    
                    // cache the results
                    if (cacheDir != null) {
	    	      FileOutputStream fos = null;
                      try {
                        String ivoid = extractID(vor, te);
                        String vorfile = renameVOR(resourceDir, ivoid,
                          ((VOResourceCache.FileIterator) di).getLastFilename());
                        File rf = new File(resultsDir, vorfile);
			fos = new FileOutputStream(rf);
                        printer.transform(new DOMSource(recresult), new StreamResult(fos));
                      }
                      catch (ClassCastException ex) {
                        throw new InternalError("programmer error: doc " + 
                                            "iterator not from VOResourceCache");
                      } finally {
			if (fos != null)
			  fos.close();
		      }
                    }

                    // add validation results to output document
                    int np = addStats(te);
                    if (np > 0 && ++nrecs < includeMax) {
                        te = (Element) out.importNode(te, true);
                    }
                    else {
                        te = (Element) out.importNode(te, false);
                    }
                    sampleroot.appendChild(te);
                    sampleroot.appendChild(out.createTextNode("\n"));

                    tellVOR(listener, status, vor, te);
                }

                // cache the XML-formatted results to disk
                if (resultsfile != null) {
                    OutputStream os = new FileOutputStream(resultsfile);
                    printer = tfact.newTransformer();
                    printer.transform(new DOMSource(out), 
                                      new StreamResult(os));
                    os.close();
                }

                return out;
            }
            catch (DOMException ex) {
                throw new ProcessingException(ex);
            }
            catch (SAXException ex) {
                throw new ProcessingException(ex);
            }
            catch (ParserConfigurationException ex) {
                throw new ProcessingException(ex);
            }
            catch (TransformerException ex) {
                throw new ProcessingException(ex);
            }
            catch (HarvestingException ex) {
                throw new ProcessingException(ex);
            }
            finally {
                doneValidating(VOR);
                tellProgress(listener, status, "completed", "VOR",
                             "completed VOResource record validation.");
            }
        }
        
    }

    /**
     * create a new result wrapper Document.  This is used to hold 
     * the root elements from validateOAI(), validateIVOAHarvest(), and/or 
     * validateVOResources() when creating a combined view of the results.  
     */
    public Document createResultsWrapperDocument() 
        throws ParserConfigurationException
    {
        Document out = df.newDocumentBuilder().newDocument();
//         out.appendChild(out.createTextNode("\n"));
        Element root = out.createElement(resultRootName);
        out.appendChild(root);
        root.setAttribute("baseURL", baseurl);
        root.appendChild(out.createTextNode("\n"));

        return out;
    }

    /**
     * run all three parts of the harvesting interface validation and 
     * return the results as a single document.  
     */
    public Document validate(int maxVORInclude, ValidaterListener listener) 
        throws TestingException, IOException, InterruptedException
    {
        NodeList children = null;
        int i = 0;

        Map status = setStatus(null);

        Document out = null;
        try {
            out = createResultsWrapperDocument();
        }
        catch (ParserConfigurationException ex) {
            throw new ConfigurationException(ex);
        }
        Element root = out.getDocumentElement();

        Document nxt = validateOAI(listener, status);
        Element nxtroot = nxt.getDocumentElement();
        root.appendChild(out.importNode(nxtroot, true));
        root.appendChild(out.createTextNode("\n\n"));

        nxt = validateIVOAHarvest(listener, status);
        nxtroot = nxt.getDocumentElement();
        root.appendChild(out.importNode(nxtroot, true));
        root.appendChild(out.createTextNode("\n\n"));

        nxt = validateVOResources(listener, status, maxVORInclude);
        nxtroot = nxt.getDocumentElement();
        root.appendChild(out.importNode(nxtroot, true));
        root.appendChild(out.createTextNode("\n"));

        tellProgress(listener, status, "done", "",
                     "Full registry validation completed.");

        if (addAssessment(root) && cacheDir != null) {
            try {
                cacheRegistryRecord();
            } catch (HarvestingException ex) {
                throw new ProcessingException(ex); 
            }
        }

        if ("pass".equals(root.getAttribute("status")) && cacheDir != null && 
            (new File(cacheDir, nm("RegResourceFile"))).exists())
        {
            root.setAttribute("allowUpload", "true");
        }

        if (cacheDir != null) {
	    OutputStream os = null;
            try {
                os = new FileOutputStream(new File(cacheDir,"Results.xml"));
                Transformer printer = tfact.newTransformer();
                printer = tfact.newTransformer();
                printer.transform(new DOMSource(out), new StreamResult(os));
            }
            catch (TransformerException ex) {
                throw new ProcessingException(ex);
            } finally {
		try { os.close(); } catch (IOException ioex) { }
	    }
        }

        return out;
    }

    Pattern identifyPat = Pattern.compile(".*\\d-Identify.xml");
    private void cacheRegistryRecord() throws IOException, HarvestingException {
        if (cacheDir != null) {
            File[] files = cacheDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return identifyPat.matcher(name).matches();
                }
            });

            if (files == null || files.length == 0) {
                complain("Identify records missing from cache!");
                return; 
            }

            Reader vor = null;
            VOResourceExtractor pullout = 
                new VOResourceExtractor(new FileReader(files[0]));
            vor = pullout.nextReader();
            if (vor != null) {
                FileWriter out = 
                    new FileWriter(new File(cacheDir, nm("RegResourceFile")));
                char[] buf = new char[16*1024];
                int n = 0;

                while ((n = vor.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
                out.close();
                vor.close();
            }
        }
    }

    /*
     * add up all the failures, warnings, and recommendations, and assess if
     * the registry is compliant enough.
     */
    private boolean addAssessment(Element root) throws ProcessingException {
        int nf = 0, nw = 0, nr = 0;
        String show = null;
        boolean good = true;
        try {
            Node child = root.getFirstChild();
            Element category = null;
            while (child != null) {
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    category = (Element) child;
                    int n = 0;
                    String nc = category.getAttribute("nfail");
                    if (nc == null || nc.length() == 0) { 
                        addStats(category);
                        nc = category.getAttribute("nfail");
                    }

                    show = category.getAttribute("showStatus");
                    if (! hasTokens(show, notGoodEnough)) good = false;

                    nf += Integer.parseInt(nc);
                    nc = category.getAttribute("nwarn");
                    nw += Integer.parseInt(nc);
                    nc = category.getAttribute("nrec");
                    nr += Integer.parseInt(nc);
                }
                child = child.getNextSibling();
            }

            root.setAttribute("nfail", (new Integer(nf)).toString());
            root.setAttribute("nwarn", (new Integer(nw)).toString());
            root.setAttribute("nrec",  (new Integer(nr)).toString());

            if ((notGoodEnough & ResultTypes.FAIL) > 0 && nf != 0) good = false;
            if ((notGoodEnough & ResultTypes.WARN) > 0 && nw != 0) good = false;
            if ((notGoodEnough & ResultTypes.REC) > 0 && nr != 0) good = false;
            root.setAttribute("status", (good) ? "pass" : "fail");

            return good;
        }
        catch (NumberFormatException ex) {
            throw new ProcessingException("Corrupted testing statistics in " + 
                                          "XML results");
        }
        catch (NullPointerException ex) {
            throw new ProcessingException("Failed to add testing statistics " + 
                                          "to XML results");
        }
    }

    /**
     * run all three parts of the harvesting interface validation and rely
     * on the cache directory for the output.  
     */
    public void cacheValidation(ValidaterListener listener) 
        throws TestingException, IOException, InterruptedException
    {
        if (cacheDir == null) 
            throw new ConfigurationException("No cache directory set");

        Map status = setStatus(null);

        validateOAI(listener, status);
        validateIVOAHarvest(listener, status);
        validateVOResources(listener, status, 1);
        status.put("done", Boolean.TRUE);
        tellProgress(listener, status, "done", "",
                     "Full registry validation completed.");
    }

    private void setForXMLValidation(DocumentBuilderFactory fact) {
        if (builtinSchemas)
          ValidationUtils.setForXMLValidation(fact, null);
        else
          ValidationUtils.setForXMLValidation(fact, sl);
    }

    private DocumentBuilder makeVOResourceParser(ParsingErrors pe) 
        throws ParserConfigurationException
    {
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        setForXMLValidation(fact);
        DocumentBuilder db = fact.newDocumentBuilder();
        if (pe != null) db.setErrorHandler(pe);
        return db;
    }

    /**
     * add a test result to a given XML document reporting an exception-
     * generating problem.
     * @param root      the element to add the result node to
     * @param tq        the TestQuery that failed
     * @param testname  the item name to give this result
     * @param message   the description of the failure
     */
    protected void addTestingFailure(Element root, TestQuery tq, 
                                     String testname, String message)
         throws DOMException
    {
        Document doc = root.getOwnerDocument();

        String rootname = queryRootName;
        if (rootname == null) 
            rootname = getCurrentQueryRootName(root);
        if (rootname == null) rootname = queryRootName;

        Element battery = doc.createElement(rootname);
        battery.setAttribute("name", tq.getName());
        battery.setAttribute("role", tq.getType());
        battery.setAttribute("showStatus", tq.getResultTokens());

        if (tq instanceof HTTPGetTestQuery) 
            battery.setAttribute("options", 
                                 ((HTTPGetTestQuery) tq).getURLArgs());

        Element test = doc.createElement("test");
        test.setAttribute("item", testname);
        test.setAttribute("status", "fail");

        test.appendChild(doc.createTextNode(message));
        battery.appendChild(test);
        root.appendChild(battery);
    }

    /**
     * add a communication error and return an appropriate user message
     */
    protected String addCommFailure(Element root, TestQuery tq, Exception ex) 
         throws DOMException
    {
        Class excl = ex.getClass();
        String exname = excl.getName().substring(
                                     excl.getPackage().getName().length()+1);

        String out = "Apparent communication error produced an exception " +
            "inside the validater: \n" + exname + ": " + ex.getMessage();

        String item = "comm";
        if (ex instanceof UnrecognizedResponseTypeException) 
            item = "responseType";

        addTestingFailure(root, tq, item, 
                          out + "\n(Is your network up? Behind a firewall?)");

        return out;
    }

    private String getCurrentQueryRootName(Element parent) {
        Element child = XSLEvaluator.getLastChildElement(parent);

        String out = null;
        if (child != null) out = child.getNodeName();
        return out;
    }

    /*
     * fill out a new status map for tracking progress of VOResource 
     * record validation 
     */
    private Map setVORStatus(Map out) {
        out = setOAIStatus(out);
        out.put("nextQueryName", "VOResource compliance");
        out.put("lastID", "");
        return out;
    }

    /*
     * fill out a new status map for tracking progress of VOResource 
     * record validation 
     */
    private Map setIVOAStatus(Map out) {
        Map newmap = statushelper.newStatus();
        if (out == null) {
            out = newmap;
        }
        else {
            Object id = out.get("id");
            out.putAll(newmap);
            out.put("id", id);
        }

        out.put("nextQueryName", "VOResource compliance");
        out.put("lastID", "");
        return out;
    }

    /*
     * fill out a new status map for tracking progress of OAI
     * validation 
     */
    private Map setOAIStatus(Map out) {
        Integer zero = new Integer(0);
        String empty = "";
        if (out == null) out = new HashMap(5);

        if (! out.containsKey("id")) out.put("id", StatusHelper.newID());
        out.put("done", Boolean.FALSE);
        out.put("ok", Boolean.TRUE);
        out.put("message", empty);
        out.put("nextQueryName", "OAI compliance");
        out.put("lastQueryName", empty);

        return out;
    }

    private void extractFromListRecords(File[] listrecs, File outdir, 
                                        String base)
        throws HarvestingException, IOException
    {
        Reader vor = null;
        FileWriter out = null;
        VOResourceExtractor pullout = null;
        StringBuffer file = null;
        char[] buf = new char[16*1024];
        int i=0, j=0, n=0;

        for(i=0; i < listrecs.length; i++) {
            pullout = new VOResourceExtractor(new FileReader(listrecs[i]));
            while ((vor = pullout.nextReader()) != null) {
                file = new StringBuffer(base);
                file.append('_').append(++j).append(".xml");
                out = new FileWriter(new File(outdir, file.toString()));

                while ((n = vor.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
                out.close();
                vor.close();
            }
        }
    }

    Pattern listRecordPat = Pattern.compile(".*\\d-ListRecords.xml");
    private File[] listListRecordsFiles(File dir) {
        return dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return listRecordPat.matcher(name).matches();
                }
            });
    }

    private void tellProgress(ValidaterListener listener, Map status, 
                              String state, String message) 
    {
        if (listener == null) return;

        String id = (String) status.get("id");

        status.put("status", state);
        status.put("message", message);

        listener.progressUpdated(id, "done".equals(state), status);
    }

    private String extractID(Document voresource, Element valresults) {
        String ivoid = null;

        if (valresults != null)
            ivoid = valresults.getAttribute("ivo-id");

        if (ivoid == null && voresource != null) {
            NodeList ids = voresource.getElementsByTagName("identifier");
            if (ids.getLength() > 0) {
                Element id = (Element) ids.item(0);
                id.normalize();
                try { ivoid = ((Text) id.getFirstChild()).getData().trim(); }
                catch (Exception ex) { }
            }
        }

        return ivoid;
    }

    /**
     * turn an IVOA identifier into a filename used to cache a record
     * to disk
     */
    public static String idToFilename(String id) {
        return pid2.matcher(pid1.matcher(id).replaceFirst("")).replaceAll("_");
    }
    private static Pattern pid1 = Pattern.compile("^\\w+://");
    private static Pattern pid2 = Pattern.compile("/");

    private String renameVOR(File dir, String id, String filename) {
        String newname = filename;
        if (id != null) {
            newname = idToFilename(id);
            File oldfile = new File(dir, filename);
            if (oldfile.exists()) {
                File newfile = new File(dir, newname);
                if (! oldfile.renameTo(newfile)) {
                    complain("failed to rename " + oldfile + " to " + newfile);
                    return filename;
                }
            }
        }

        return newname;
    }

    private void tellVOR(ValidaterListener listener, Map status, 
                         Document voresource, Element valresults)
    {
        if (listener == null) return;
        if (valresults == null) {
            System.err.println("HarvestValidater: lost VOResource validation " +
                               "results");
            return;
        }

        String ivoid = extractID(voresource, valresults);
        if (ivoid == null) {
            // complain
            System.err.println("HarvestValidater: no identifier found in " +
                               "VOResource record.");
            return;
        }

        String rstatus = valresults.getAttribute("status");

        status.put("query", ivoid);
        status.put("lastQueryName", ivoid);
        status.put("rstatus", rstatus);
        String[] cat = { "fail", "warn", "rec" };
        for(int i=0; i < cat.length; i++) {
            try { 
                int n = Integer.parseInt(valresults.getAttribute("n"+cat[i]));
                status.put(cat[i], new Integer(n));
            }
            catch (NumberFormatException ex) { }
        }

        tellProgress(listener, status, "completed", ivoid, 
                     "validated " + ivoid + ".");
    }

    private int addStats(Element te) {
        String status = null;
        int nf=0, nw=0, nr=0;
        NodeList tests = te.getElementsByTagName("test");
        for(int i=0; i < tests.getLength(); i++) {
            status = ((Element) tests.item(i)).getAttribute("status");
            if ("fail".equals(status)) 
                nf++;
            else if ("warn".equals(status)) 
                nw++;
            else if ("rec".equals(status)) 
                nr++;
        }

        te.setAttribute("nfail", (new Integer(nf)).toString());
        te.setAttribute("nwarn", (new Integer(nw)).toString());
        te.setAttribute("nrec",  (new Integer(nr)).toString());

        return Math.max(nf, Math.max(nw, nr));
    }

    private Element getTestElement(Element parent) {
        Node child = parent.getLastChild();
        while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
            child = child.getPreviousSibling();
        }

        return ((Element) child);
    }

    private String nm(String key) {  return names.getProperty(key);  }

    protected void tellIVOAProgress(ValidaterListener listener, Map status, 
                                    String state, String query, String desc) 
    {
        StringBuffer sb = new StringBuffer(query);
        if (desc != null && desc.length() > 0)
            sb.append(" (").append(desc).append(")");
        sb.append(' ').append(state).append('.');
        tellProgress(listener, status, state, query, sb.toString());
    }

    protected void tellProgress(ValidaterListener listener, Map status, 
                                String state, String query, String message) 
    {
        if (listener == null) return;

        String id = (String) status.get("id");

        status.put("status", state);
        status.put("message", message);
        status.put("query", query);

        listener.progressUpdated(id, "done".equals(state), status);
    }

    private Map setStatus(Map out) {
        Integer zero = new Integer(0);
        String empty = "";
        if (out == null) out = new HashMap(5);

        if (! out.containsKey("id")) out.put("id", StatusHelper.newID());
        out.put("done", Boolean.FALSE);
        out.put("ok", Boolean.TRUE);
        out.put("message", empty);
        out.put("status", "running");
        out.put("query", empty);

        return out;
    }

    private boolean hasTokens(String show, int flags) {
        if (show == null) return false;

        if ((flags & ResultTypes.FAIL) > 0 && 
            show.indexOf(defResultTypes.getToken(ResultTypes.FAIL)) < 0)
          return false;
        if ((flags & ResultTypes.WARN) > 0 && 
            show.indexOf(defResultTypes.getToken(ResultTypes.WARN)) < 0)
          return false;
        if ((flags & ResultTypes.REC) > 0 && 
            show.indexOf(defResultTypes.getToken(ResultTypes.REC)) < 0)
          return false;

        return true;
    }

    /*
     * this produces a look-up of names for files and XML element names that
     * we want to make configurable and centrally defined, rather than 
     * hard-coded in the depths of the code.  
     */
    private Properties loadNames(Configuration config) {
        String[] defaults = {
            // general
            "testQueryElem",           "testQuery",
            "RIValRootElem",           "RegistryValidation",
            "RegResourceFile",         "Registry.xml",

            // for OAI validation
            "ExplorerOutputFile",      "OAIExplorer.txt", 
            "OAIValResultsFile",       "OAIResults.xml",
            "OAIResultRootElem",       "OAIValidation",
            "OAIResultQueryElem",      "testQuery",

            // for IVOA harvest validation
            "HarvestResponseBase",     "q",
            "HarvestValResultsFile",   "HarvestResults.xml",
            "HarvestResultElem",       "HarvestValidation",

            // for VOResource validation
            "VORValSummaryFile",       "VORResults.xml",
            "VORRecordDir",            "voresources",
            "VORResultsDir",           "vorvalidated",
            "VORRecordBase",           "vor",
            "VORResultElem",           "VOResourceValidation",
            "VORRecValElem",           "testedResource"
        };

        int i = 0;
        Properties defNames = new Properties();
        for(i=0; i < defaults.length; i += 2) 
            defNames.setProperty(defaults[i], defaults[i+1]);

        Configuration names = config.getConfiguration("names", null, null);

        Properties out = new Properties(defNames);
        for(i=0; i < defaults.length; i += 2) {
            String nm = names.getParameter(defaults[i]);
            if (nm != null) out.setProperty(defaults[i], nm);
        }

        return out;
    }

}
