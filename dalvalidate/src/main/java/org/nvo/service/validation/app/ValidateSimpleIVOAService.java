package org.nvo.service.validation.app;

import org.nvo.service.validation.SimpleIVOAServiceValidater;
import org.nvo.service.validation.ValidaterListener;
import org.nvo.service.validation.ResultTypes;
import org.nvo.service.validation.TestingException;
import org.nvo.service.validation.ConfigurationException;

import net.ivoa.util.Configuration;
import ncsa.horizon.util.CmdLine;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Hashtable;
import java.util.Enumeration;
import java.net.MalformedURLException;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException; 
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;


/**
 * a command-line tool for testing a "Simple" IVOA standard service.  
 *
 * In IVOA parlence, "simple" refers to a particular set of standard data 
 * access protocols that share common design characteristics, including:
 * <ul>
 *   <li> a 2-step query-then-retrieve interaction pattern, </li>
 *   <li> an input query that can be expressed as an HTTP Get request with 
 *        URL arguments, and </li>
 *   <li> query constraints expressed as a concatonation of input arguments 
 *        of the form, key=value </li>
 * </ul>
 * Current examples include Simple Cone Search (CSC), Simple Image Access 
 * (SIA), Simple Spectral Access (SSA).  Given the proper configuration file,
 * this tool can validate any of these types.  
 */
public class ValidateSimpleIVOAService extends CmdLine 
    implements ValidaterListener 
{
    public static final String commonCmdLineConfig = "P:C:o:sqvS:f:ewrp";
    public static final String defaultProg = 
        System.getProperty("validation.appName", "java " + 
                           ValidateSimpleIVOAService.class.toString());

    int verbosity = 0;
    ResultTypes rt = new ResultTypes();
    String prog = defaultProg;
    TransformerFactory tf = TransformerFactory.newInstance();
    String baseURL = null;
    Hashtable templates = new Hashtable();
    String usage = getUsage(prog);

    /**
     * create a tool instance
     */
    public ValidateSimpleIVOAService() {  
        this("");
    }

    /**
     * create a tool instance
     * @param args    the command line arguments for the tool.  This is usually
     *                   the input String array provided this the application's
     *                   main() method.
     */
    public ValidateSimpleIVOAService(String[] args) 
         throws CmdLine.UnrecognizedOptionException
    {  
        this();  
        if (args != null) setCmdLine(args);
    }
    
    /**
     * create a tool instance. See the {@link ncsa.horizon.util.CmdLine CmdLine}
     * for the syntax expected for the command line configuration.
     * @param extraCmdLineConfig  additional command-line configuration.  These
     *                              will be appended onto the default 
     *                              configuration.
     */
    public ValidateSimpleIVOAService(String extraCmdLineConfig) {
        this(commonCmdLineConfig + extraCmdLineConfig, NULLFLAG);
    }

    /**
     * create a tool instance.  This is used by subclasses.  
     */
    protected ValidateSimpleIVOAService(String cmdLineConfig, int flags) {
        super(cmdLineConfig, NULLFLAG);
    }

    /**
     * execute this tool the currently set arguments.  This method differs 
     * from run in that it catches all exceptions and prints error messages 
     * accordingly.  
     * @return int   a return status appropriate for passing to System.exit().
     */
    public int execute() {
        try {
            run();
        }
        catch (ConfigurationException ex) {
            if (! isSilent()) {
                printErr("Configuration Error: " + ex.getMessage());
                if (isVerbose()) ex.printStackTrace();
                return 2;
            }
        }
        catch (Exception ex) {
            if (! isSilent()) {
                printErr(ex.getMessage());
                if (isVerbose()) ex.printStackTrace();
                return 1;
            }
        }

        return 0;
    }

    /**
     * return true if the tool is set to be silent.  If true, no
     * output is written to either standard out or error.
     */
    public boolean isSilent() { return verbosity < -1; }

    /**
     * return true if the tool is set to be somewhat quiet.  If true, friendly
     * progress messages will not be written out to standard error.  
     */
    public boolean isQuiet() { return verbosity < 0; }

    /**
     * return true if the tool is set to be verbose in its messages to 
     * standard error.  If true, stack traces from any caught exceptions 
     * will be printed out.
     */
    public boolean isVerbose() { return verbosity > 0; }

    protected void setCmdLine(String[] args, boolean doconfigure) 
        throws CmdLine.UnrecognizedOptionException 
    {
        super.setCmdLine(args);
        if (doconfigure) configure();
    }

    public void setCmdLine(String[] args) 
        throws CmdLine.UnrecognizedOptionException 
    {
        setCmdLine(args, true);
    }

    /**
     * analyze the command line arguments and configure this tool accordingly
     */
    protected void configure() {
        setVerbosity();
        setResultTypes();
        setBaseURL();

        if (isSet('P')) prog = getValue('P');
        usage = getUsage(prog);
    }

    protected void setVerbosity() {
        if (isSet('s')) 
            verbosity = -2;
        else if (isSet('q')) 
            verbosity = -1;
        else if (isSet('v')) 
            verbosity = 1;
        else
            verbosity = 0;
    }

    protected void setResultTypes() {
        if (isSet('f')) rt.addTypes(ResultTypes.FAIL);
        if (isSet('w')) rt.addTypes(ResultTypes.WARN);
        if (isSet('r')) rt.addTypes(ResultTypes.REC);
        if (isSet('p')) rt.addTypes(ResultTypes.PASS);
        if (rt.getTypes() == 0) rt.addTypes(ResultTypes.ADVICE);
    }

    public void printErr(String msg) {
        if (prog != null && prog.length() != 0) {
            System.err.print(prog);
            System.err.print(": ");
        }
        System.err.println(msg);
    }

    protected void setBaseURL() {
        Enumeration args = arguments();
        if (! args.hasMoreElements())
            throw new IllegalArgumentException("No baseURL provided");

        baseURL = (String) args.nextElement();
    }

    protected Configuration getConfiguration() 
         throws IOException, SAXException 
    {
        String configfile = System.getProperty("validation.configfile");
        if (isSet('C')) configfile = getValue('C');
        if (configfile == null || configfile.trim().length() <= 0)
            throw new IllegalArgumentException("no config file specified.");
        
        return new Configuration(configfile, this.getClass());
    }

    public void progressUpdated(String id, boolean done, Map status) {
        if (! isQuiet()) {
            String message = (String) status.get("message");
            if (message != null && message.length() > 0) 
                System.err.println(status.get("lastQueryName") + ": " +
                                   message);
            if (! ((Boolean) status.get("done")).booleanValue()) {
                System.err.print("testing ");
                System.err.print(status.get("nextQueryName"));
                String desc = 
                    (String) status.get("nextQueryDescription");
                if (desc != null && desc.length() > 0) {
                    System.err.print(" (");
                    System.err.print(desc);
                    System.err.print(")");
                }
                System.err.println("...");
            }
            else 
                System.err.println("testing complete.");
        }
    }

    protected Transformer getTransformerForFormat(Configuration config, 
                                                  String format) 
         throws FileNotFoundException, IOException,
                TransformerConfigurationException
    {
        if (format != null && format.length() == 0) format = null;

        Templates stylesheet = (Templates) templates.get(format);
        if (stylesheet == null) {
            String ssfile = config.getParameter("resultStylesheet", "format",
                                                format); 
            if (ssfile == null && "xml".equals(format)) ssfile = "";
            if (ssfile == null) return null;

            if (ssfile.length() == 0) return tf.newTransformer();

            stylesheet = tf.newTemplates(
               new StreamSource(Configuration.openFile(ssfile, this.getClass()))
            );
        }
            
        return stylesheet.newTransformer();
    }

    public void run() 
         throws TestingException, DOMException, MalformedURLException, 
                IOException, ParserConfigurationException, SAXException,
                TransformerConfigurationException, TransformerException
    {
        Configuration config = getConfiguration();

        SimpleIVOAServiceValidater validater = 
                new SimpleIVOAServiceValidater(config, tf, this.getClass());

        String format = "text";
        if (isSet('f')) format = getValue('f');
        Transformer printer = getTransformerForFormat(config, format);
        if (printer == null) 
            throw new IllegalArgumentException("unsupported format: " + format);

        Document result = validater.validate(baseURL, null, this);

        OutputStream out = System.out;
        if (isSet('o')) {
            String file = getValue('o');
            out = new FileOutputStream(file);
        }
        
        if (! isSilent() || isSet('o'))
            printer.transform(new DOMSource(result), new StreamResult(out));
    }

    public static String getUsage(String prog) {
        StringBuffer sb = new StringBuffer("Usage: ");
        sb.append(prog).append(" [ -ewrpsqv ] [-o outfile] [-f format]");
        sb.append(" [-C config] baseURL");
        return sb.toString();
    }

    public static void main(String[] args) {
        ValidateSimpleIVOAService tool = null;
        try {
            tool = new ValidateSimpleIVOAService(args);
            System.exit(tool.execute());
        }
        catch (CmdLine.UnrecognizedOptionException ex) {
            System.err.println(getUsage(defaultProg));
            System.exit(1);
        }
        catch (Exception ex) {
            tool = new ValidateSimpleIVOAService();
            tool.printErr(ex.getMessage());
            System.exit(1);
        }

    }
}


