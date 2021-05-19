package org.nvo.service.validation;

import net.ivoa.util.Configuration;

import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TestVOTXSLEvaluator {

   public static void main(String[] args) {

      VOTableXSLEvaluator eval = null;
      HTTPGetTestQuery tq = null;
      ResultTypes rt = new ResultTypes(ResultTypes.ADVICE);
      int count = 0;

      try {
        Document resdoc = 
         DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element results = resdoc.createElement("XSLEvaluation");
        resdoc.appendChild(results);

        if (args.length < 1) {
            eval = new VOTableXSLEvaluator();
            eval.useStylesheet("v1.1", "data/checkConeSearch-v1_1.xsl");

            tq = new HTTPGetTestQuery("http://conesearch.edu/cgi-bin/cs.pl",
                                      "RA=180.0&DEC=60.0&SR=1.0", 
                                      "conesearch", "user", "Test query", rt, 
                                      new Properties());
            // tq.setResultTypes(ResultTypes.PASS);

            InputStream response = 
                ClassLoader.getSystemResourceAsStream("data/vocone-v1_1.xml");

            count = eval.applyTests(response, tq, results);
        }
        else {
            String configfile = args[0];

            String urlbase = null;
            if (args.length > 1) urlbase = args[1];

            Configuration config = new Configuration(configfile);
            Configuration tqconfig = 
                HTTPGetTestQuery.getTestQueryConfig(config, "httpget");

            if (urlbase == null) 
                urlbase = HTTPGetTestQuery.getBaseURL(tqconfig, null);
            if (urlbase == null) {
                System.err.println("No base URL configured; " +
                                   "please provide one on the command-line");
                System.exit(1);
            }

            HTTPGetTestQuery.QueryData qd = 
                HTTPGetTestQuery.getQueryData(tqconfig, null);
            tq = new HTTPGetTestQuery(urlbase, qd.args, qd.name, qd.type,
                                      qd.desc, rt, new Properties());

            Configuration evalconfig = 
                config.getConfiguration("evaluator", "type", "xsl");
            eval = new VOTableXSLEvaluator(evalconfig, null, null);

            count = eval.applyTests(tq, results);
        }

        results.setAttribute("count", Integer.toString(count));
        
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer printer = tf.newTransformer();
        printer.transform(new DOMSource(resdoc), 
                          new StreamResult(System.out));
        
      }
      catch (Exception ex) {
          System.err.println("Error: " + ex.getMessage());
          ex.printStackTrace();
          System.exit(1);
      }
   }
}
