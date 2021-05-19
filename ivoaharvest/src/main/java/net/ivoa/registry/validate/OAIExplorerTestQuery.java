package net.ivoa.registry.validate;

import net.ivoa.util.Configuration;

import org.nvo.service.validation.ResultTypes;
import org.nvo.service.validation.TestQueryBase;
import org.nvo.service.validation.QueryConnection;
import org.nvo.service.validation.HTTPGetQueryConnection;
import org.nvo.service.validation.ValidaterListener;
import org.nvo.service.validation.StatusHelper;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

/**
 * the suite of standard test queries from the OAI Explorer tool/service 
 * bundled as a single TestQuery.  
 * 
 * <p>
 * OAI Explorer is a tool for validating an OAI-PMH service.  It sends the 
 * service a number of queries and returns the result back as text.  This 
 * class can invoke this tool either as a local, standalone executable (which 
 * is currently a compiled C program) or as a remote web service.  
 */
public class OAIExplorerTestQuery extends TestQueryBase {

    /**
     * the default web service URL for the OAI Explorer Tool
     */
    public final static String testerURLBase = 
        "http://re.cs.uct.ac.za/cgi-bin/Explorer/2.0-1.46/addarchive?" +
        "language=enus.lan&archiveurl=";

    URL baseURL = null;
    String testerURL = testerURLBase;
    File testercmd = null;

    /**
     * create the test query with a base URL
     * @param oaiURL   the base URL for the OAI service being tested
     */
    public OAIExplorerTestQuery(URL oaiURL) {
        super("OAI-PMH", "oai", 
              "tests compliance with the general OAI-PMH standard",
              null, null);
        baseURL = oaiURL;
        setResultTypes(ResultTypes.ADVICE);
    }

    /**
     * create the test query with a base URL that will employ the web service
     * version of the OAI Explorer Tool.  
     * @param oaiURL   the base URL for the OAI service being tested
     * @param explorerURL  the URL for the web service version of the OAI
     *                   Explorer tool.  If null, the web service version will
     *                   be used via the default location.  
     */
    public OAIExplorerTestQuery(URL oaiURL, String explorerURL) {
        this(oaiURL);
        testerURL = explorerURL;
    }

    /**
     * create the test query with a base URL and an optional File location
     * to cache the results to.
     * @param oaiURL   the base URL for the OAI service being tested
     * @param explorerCmd  the location of a locally executable version of the 
     *                   Explorer tool.  If null, the web service version will
     *                   be used via the default location.  
     */
    public OAIExplorerTestQuery(URL oaiURL, File explorerCmd) {
        this(oaiURL);
        testercmd = explorerCmd;
    }

    /**
     * invoke the query and return the stream carrying the response
     * @exception IOException   if an error occurs while opening a stream.
     */
    public QueryConnection invoke() throws IOException {
        return (testercmd != null) 
            ? (QueryConnection) new CmdConn(baseURL, testercmd) 
            : (QueryConnection) new  WSConn(baseURL, testerURL);
    }

    /**
     * extract a testQuery sub-configuration from the given Configuration
     * @param config  the Configuration to extract from.
     * @param name    select the one with the given name.  If null, the 
     *                  first one with the name "oaiexplorer" will be 
     *                  returned.
     */
    public static Configuration getTestQueryConfig(Configuration config, 
                                                   String name) 
    {
        if (name == null) name = "oaiexplorer";
        return config.getConfiguration("testQuery", "name", name);
    }

    class CmdConn implements QueryConnection {

        URL baseURL = null;
        File testerCmd = null;
        Process proc = null;

        public CmdConn(URL oaiURL, File explorerCmd) {
            baseURL = oaiURL;
            testerCmd = explorerCmd;
        }

        public InputStream getStream() throws IOException {
	    String languageFile = testercmd.getAbsoluteFile().getParentFile() + "/enus.lan";
            String[] cmd = { testercmd.getAbsolutePath(), baseURL.toString(), languageFile }; 
            File wdir = testercmd.getAbsoluteFile().getParentFile();

            proc = Runtime.getRuntime().exec(cmd, null, wdir);
            return new CmdStream();
        }

        public boolean waitUntilReady(long timeout) {  return true;  }
        public boolean isStreamReady() {  return true;  }

        public void shutdown() {
            if (proc != null) { proc.destroy(); }
        }

        // this class will capture any error output from the stream and 
        // throw it as an exception within the thread using the input 
        // stream.  
        class CmdStream extends FilterInputStream {
            int exit = 0;

            public CmdStream() {
                super(proc.getInputStream());
            }

            public int read() throws IOException {
                try {
                    int out = super.read();
                    if (out < 0) checkExitStatus();
                    return out;
                }
                catch (IOException ex) {
                    checkExitStatus();
                    throw ex;
                }
            }

            public int read(byte[] b, int off, int len) throws IOException {
                try {
                    int out = super.read(b, off, len);
                    if (out < 0) checkExitStatus();
                    return out;
                } 
                catch (IOException ex) {
                    checkExitStatus();
                    throw ex;
                }
            }

            void checkExitStatus() throws IOException {
                if (proc != null) {
                    try {
                        exit = proc.exitValue();
                    }
                    catch (IllegalThreadStateException ex) {
                        // shouldn't happen
                        try { proc.waitFor(); }
                        catch (InterruptedException e) {
			  proc.destroy(); 
			  proc = null;
			  return; 
			}
                        exit = proc.exitValue();
                    }
                    if (exit > 1) {
                        BufferedReader br = 
                            new BufferedReader(new InputStreamReader(
                                proc.getErrorStream()));
                        StringBuffer sb = new StringBuffer();
                        String line = null;
                        while ((line = br.readLine()) != null) { 
                            sb.append(line); 
                        }
                        throw new IOException("OAI-Explorer command failure: " + 
                                              sb.toString());
                    }

                    // now forget we were ever running this.  
                    proc = null;
                }
            }
        }
    }

    class WSConn implements QueryConnection {
        HTTPGetQueryConnection delegate = null;

        public WSConn(URL oaiURL, String explorerURL) throws IOException {
            try {
                URL tester = new URL(explorerURL + 
                                     URLEncoder.encode(oaiURL.toString(), 
                                                       "UTF-8"));
                delegate = new HTTPGetQueryConnection(tester);
            } catch (UnsupportedEncodingException ex) {
                throw new InternalError("Java problem: not UTF-8 encoding " +
                                        "available: " + ex.getMessage());
            }
        }

        public InputStream getStream() throws IOException, InterruptedException {
            return delegate.getStream();
        }

        public boolean waitUntilReady(long timeout) 
            throws IOException, InterruptedException 
        {
            return delegate.waitUntilReady(timeout);
        }

        public boolean isStreamReady() {  return delegate.isStreamReady();  }

        public void shutdown() throws IOException {
            try {
                delegate.shutdown();
            }
            finally {
                delegate = null;
            }
        }
    }
    
}
