package org.nvo.service.validation;

import net.ivoa.util.Configuration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

public class TestCachingDOMTester {

    CachingDOMTester tester = null;

    public TestCachingDOMTester(TestQuery tq, Evaluator eval, File cache) {
        tester = new CachingDOMTester(tq, eval, cache);
    }

    public int run(Element results) throws Exception {
        return tester.applyTests(results);
    }

    public int runAndShow() throws Exception {
        Document resdoc = 
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                                  .newDocument();
        Element results = resdoc.createElement("XSLEvaluation");
        resdoc.appendChild(results);

        int count = run(results);
        count += run(results);

        results.setAttribute("count", Integer.toString(count));
        
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer printer = tf.newTransformer();
        printer.transform(new DOMSource(resdoc), 
                          new StreamResult(System.out));

        return count;
    }

    public static TestCachingDOMTester makeHTTPGetTester(Configuration config, 
                                                         String baseurl, 
                                                         File file,
                                                         ResultTypes rt) 
         throws Exception
    {
        // create the Evaluator
        Configuration econfig = config.getConfiguration("evaluator", "type", 
                                                        "xsl");
        if (econfig == null) econfig = Configuration.makeEmpty("evaluator");
        Evaluator eval = new VOTableXSLEvaluator(econfig, null, null);

        // create the TestQuery
        Configuration tqconfig = 
                HTTPGetTestQuery.getTestQueryConfig(config, "httpget");
        if (tqconfig == null)
            throw new IllegalArgumentException("No testQuery configuration " + 
                                               "found");

        if (baseurl == null) 
            baseurl = HTTPGetTestQuery.getBaseURL(tqconfig, null);
        if (baseurl == null)
            throw new IllegalArgumentException("No base URL configured; " +
                                               "please provide a non-null " +
                                               "value explicitly");

        HTTPGetTestQuery query = 
            new HTTPGetTestQuery(baseurl, new Properties());
        if (rt != null) query.setResultTypes(rt.getTypes());
        query.setQueryData(HTTPGetTestQuery.getQueryData(tqconfig, null));

        TestCachingDOMTester out = new TestCachingDOMTester(query, eval, file);
        return out;
    }

    public static void main(String[] args) {
        ResultTypes rt = new ResultTypes(ResultTypes.ADVICE);

        try {
            if (args.length < 1) 
                throw new IllegalArgumentException("missing config file");
            String configfile = args[0];

            String urlbase = null;
            if (args.length > 1) urlbase = args[1];

            String filename = "TestCachingTester.data";
            if (args.length > 2) filename = args[2];

            Configuration config = new Configuration(configfile);

            TestCachingDOMTester testertester = 
                makeHTTPGetTester(config, urlbase, new File(filename), rt);
            testertester.runAndShow();
        }
        catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
