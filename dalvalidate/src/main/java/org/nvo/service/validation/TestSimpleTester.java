package org.nvo.service.validation;

import net.ivoa.util.Configuration;

import java.util.List;
import java.util.LinkedList;
import java.util.Vector;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

public class TestSimpleTester {

    SimpleTester tester = null;
    Vector queries = new Vector();
    int index = 0;

    public TestSimpleTester(TestQuery tq, Evaluator eval) {
        addQuery(tq);
        tester = new SimpleTester(tq, eval);
    }

    public void addQuery(TestQuery q) {  queries.add(q); }

    public int runNext(Element result) throws Exception {
        ++index;
        if (index >= queries.size()) index = 0;
        int out = tester.applyTests(result);
        tester.setTestQuery((TestQuery) queries.elementAt(index));
        return out;
    }

    public int runAll(Element result) throws Exception {
        int lim = queries.size();
        int count = 0;
        for(int i = 0; i < lim; i++) {
            count += runNext(result);
            System.err.println("Returned " + count + 
                               " test results from test query no. " + (i+1));
        }
        return count;
    }

    public static TestSimpleTester makeHTTPGetTester(Configuration config, 
                                                     String baseurl, 
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

        LinkedList qd = new LinkedList();

        HTTPGetTestQuery.addAllQueryData(tqconfig, qd);
        if (qd.size() == 0) 
            throw new IllegalArgumentException("no queries found in the " + 
                                               "inputs");

        HTTPGetTestQuery query = 
            new HTTPGetTestQuery(baseurl, new Properties());
        if (rt != null) query.setResultTypes(rt.getTypes());
        query.setQueryData((HTTPGetTestQuery.QueryData) qd.removeFirst());

        TestSimpleTester out = new TestSimpleTester(query, eval);
        HTTPGetTestQuery tq = null;
        while (qd.size() > 0) {
            tq = new HTTPGetTestQuery(baseurl, query, true);
            tq.setQueryData((HTTPGetTestQuery.QueryData) qd.removeFirst());
            out.addQuery(tq);
        }

        return out;
    }

    public int runAndShow() throws Exception {
        Document resdoc = 
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                                  .newDocument();
        Element results = resdoc.createElement("XSLEvaluation");
        resdoc.appendChild(results);

        int count = runAll(results);

        results.setAttribute("count", Integer.toString(count));
        
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer printer = tf.newTransformer();
        printer.transform(new DOMSource(resdoc), 
                          new StreamResult(System.out));

        return count;
    }

    public static void main(String[] args) {

        ResultTypes rt = new ResultTypes(ResultTypes.ADVICE);

        try {
            if (args.length < 1) 
                throw new IllegalArgumentException("missing config file");
            String configfile = args[0];

            String urlbase = null;
            if (args.length > 1) urlbase = args[1];

            Configuration config = new Configuration(configfile);

            TestSimpleTester testertester = 
                makeHTTPGetTester(config, urlbase, rt);
            testertester.runAndShow();
            testertester.runAndShow();
        }
        catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
