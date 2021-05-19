package ncsa.xml.validation;

import ncsa.horizon.util.CmdLine;

import java.io.Reader;
import java.io.FileReader;
import java.io.File;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;

/**
 * an application that will validate an XML document against the XML Schemas 
 * that it references.  
 */
public class Validate {

    DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();

    /**
     * validate a list of files
     */
    public static void main(String[] args) {
        CmdLine cl = new CmdLine("qshS:");
        try {
            cl.setCmdLine(args);
        }
        catch (CmdLine.UnrecognizedOptionException ex) {
            System.err.println(ex);
            usage(System.err);
            System.exit(2);
        }

        // print usage and exit
        if (cl.isSet('h')) {
            usage(System.err);
            System.exit(0);
        }
        boolean silent = cl.isSet('s');
        boolean quiet = cl.isSet('q');
        if (silent) quiet = silent;

        // set a schema location file
        SchemaLocation sl = null;
        if (cl.isSet('S')) {
            File slfile = new File(cl.getValue('S'));
            try {
                sl = new SchemaLocation();
                sl.load(slfile);
            }
            catch (FileNotFoundException ex) {
                if (! silent)
                    System.err.println("Schema location file not found: " + 
                                       slfile);
                System.exit(2);
            }
            catch (IOException ex) {
                if (! silent)
                    System.err.println("Trouble reading schema location file: " +
                                       ex.getMessage() + ": " + slfile);
                System.exit(2);
            }
        }

        Validate v = new Validate(sl);

        PrintWriter out = null;
        if (! quiet) out = new PrintWriter(System.out, true);
        int exit = 0;
        String xmlfile = null;

        for(Enumeration e = cl.arguments(); e.hasMoreElements();) {
            try {
                xmlfile = (String) e.nextElement();
                if (v.validate(new FileReader(xmlfile), out)) {
                    if (out != null) {
                        out.print(xmlfile);
                        out.println(": valid!");
                    }
                }
                else {
                    if (out != null) {
                        System.out.print(xmlfile);
                        System.out.println(": not valid.");
                    }
                    exit = 1;
                }
            } catch (Exception ex) {
                if (! silent) {
                    System.err.print("Validation failed: ");
                    System.err.println(ex.getMessage());
                }
                System.exit(2);
            }
        }

        System.exit(exit);
    }

    /**
     * create the validater
     */
    public Validate(SchemaLocation sl) {
        if (sl == null) sl = new SchemaLocation(getClass());

        ValidationUtils.setForXMLValidation(fact, sl);
    }

    /**
     * create the validater
     */
    public Validate() {
        this(null);
    }

    /**
     * validate the XML document on the given stream
     */
    public boolean validate(Reader doc, PrintWriter errors) 
         throws ParserConfigurationException, IOException
    {
        DocumentBuilder db = fact.newDocumentBuilder();
        Handler eh = new Handler(errors);
        db.setErrorHandler(eh);

        try {
            Document result = db.parse(new InputSource(doc));
            return (result != null && eh.okay);
        }
        catch (SAXException ex) {
            eh.print("ABORT", ex);
            return false;
        }
    }

    class Handler extends DefaultHandler {
        PrintWriter out = null;
        Locator loc = null;
        boolean okay = true;

        public Handler(PrintWriter errors) {
            out = errors;
        }

        public void setDocumentLocator(Locator locator) {
            loc = locator;
        }

        public void error(SAXParseException ex) {
            okay = false;
            print("ERROR", ex);
        }

        public void warning(SAXParseException ex) {
            print("WARNING", ex);
        }

        public void fatalError(SAXParseException ex) {
            okay = false;
            print("FAILURE", ex);
        }

        void print(String level, SAXException ex) {
            if (out != null) {
                out.print('[');
                out.print(level);
                out.print(']');

                if (loc != null) {
                    String file = loc.getSystemId();
                    int index = -1;
                    if (file != null && (index=file.lastIndexOf('/')) > -1) 
                        file = file.substring(index+1);

                    out.print(' ');
                    out.print(file);
                    out.print(':');
                    out.print(loc.getLineNumber());
                    out.print(':');
                    out.print(loc.getColumnNumber());
                }
                out.print(": ");
                out.println(ex.getMessage());
            }
        }
    }

    /**
     * print the usage message to a stream
     * @param out    the stream to write to
     */
    public static void usage(PrintStream out) {
        out.println("validate [ -qh ] [ -S schemaLocFile ] xmlfile ...");
        out.println("  -h      print this usage (ignore all other input)");
        out.println("  -q      print nothing to standard out; only set " + 
                              "the exit code");
        out.println("  -s      print nothing to standard out or error; only " + 
                              "set the exit code");
        out.println("  -S schemaLocFile  set the schema cache via a schema " +
                              "location file");
        out.println("Each line in a schemaLocFile gives a namespace, a space, " +
                    "and local file path.");
        out.println("The file path is the location of the Schema (.xsd) document"
                    + " for that namespace.");
    }
}
