package org.nvo.service.validation;

import net.ivoa.util.Configuration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory; 
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

public class TestSimpleIVOAServiceValidater {

    public static void main(String[] args) {

        try {
            if (args.length < 1) 
                throw new IllegalArgumentException("missing config file");
            String configfile = args[0];

            String urlbase = null;
            if (args.length > 1) urlbase = args[1];

            Configuration config = new Configuration(configfile);
            if (urlbase == null) {
                Configuration tqconfig = 
                    HTTPGetTestQuery.getTestQueryConfig(config, "httpget");
                if (tqconfig == null) throw 
                    new ConfigurationException("No testQuery configuration " +
                                               "found");
                urlbase = HTTPGetTestQuery.getBaseURL(tqconfig, null);
            }
            if (urlbase == null) throw 
                new IllegalArgumentException("No base URL configured; " +
                                             "please provide a non-null " +
                                             "value explicitly");
        
            SimpleIVOAServiceValidater validater = 
                new SimpleIVOAServiceValidater(config);

            ValidaterListener monitor = new ValidaterListener() {
                public void progressUpdated(String id, boolean done, Map status)
                {
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
                        System.err.println();
                    }
                    else 
                        System.err.println("testing complete.");
                }
            };

            Document out = validater.validate(urlbase, null, monitor);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer printer = tf.newTransformer();
            printer.transform(new DOMSource(out), 
                              new StreamResult(System.out));
        }
        catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
