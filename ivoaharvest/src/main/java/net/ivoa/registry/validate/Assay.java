package net.ivoa.registry.validate;

import ncsa.xml.validation.SchemaLocation;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.parsers.DocumentBuilder;


/**
 * a wrapper around VOResource validation results that enables an assessment
 * of the results.  An Assay is created by a VOResourceAssessor.  An
 * instance that provide access to the individual issues that were discovered
 * as well as provide summary information.  It can also write out its results
 * to an (XML-formatted) file.  
 */
public class Assay {

    final static String[] statustypes = { "fail", "warn", "rec", "pass" };
    Document doc = null;
    Document results = null;
    Element resultRoot = null;
    int[] ntype = { -1, -1, -1, -1 };
    int ncount = -1;
    Transformer xmlwrtr = null;

    Assay(Document valresults, int testCount, Transformer xmlwriter) {
        results = valresults;
        resultRoot = getResultRoot(results);
        if (resultRoot != null) {
            addCounts(resultRoot, testCount);
        }
        ncount = testCount;
        xmlwrtr = xmlwriter;
    }

    void prepForWriting() {
        try {
            if (xmlwrtr == null) 
                xmlwrtr = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException ex) {
            throw new InternalError("XML Config problem: "+ex.getMessage());
        }
    }        

    Element getResultRoot(Document results) {
        try {
            Node root = results.getFirstChild();
            Node child = root.getLastChild();
            while (child != null && child.getNodeType() != root.ELEMENT_NODE) 
                child = child.getPreviousSibling();
            if (child == null) return null;
            return ((Element) child);
        }
        catch (DOMException ex) {
            return null;
        }
    }

    void addCounts(Element root, int testCount)  {
        root.setAttribute("ntest", Integer.toString(testCount));
        Iterator<Element> iter = new TestIter();
        Element child = null;
        String status = null;
        while (iter.hasNext()) {
            child = iter.next();
            status = child.getAttribute("status");
            for(int i=0; i < statustypes.length; ++i) {
                if (statustypes[i].equals(status)) {
                    if (ntype[i] < 0) ntype[i] = 0;
                    ntype[i]++;
                    break;
                }
            }
        }

        root.setAttribute("nfail", Integer.toString(ntype[0]));
        root.setAttribute("nwarn", Integer.toString(ntype[1]));
        root.setAttribute("nrec", Integer.toString(ntype[2]));
    }

    /**
     * return the parsed VOResource document
     */
    public Document getResourceRecord() { return doc; }

    /**
     * write the resource record to a file
     */
    public void writeResource(File out) throws IOException {
        if (doc == null) 
            throw new IllegalStateException("resource record not available");
        writeDoc(doc, out);
    }

    /**
     * write the resource record to a stream
     */
    public void writeResource(Writer out) throws IOException {
        if (doc == null) 
            throw new IllegalStateException("resource record not available");
        writeDoc(doc, out);
    }

    /**
     * write the validation report to a stream
     */
    public void writeReport(Writer out) throws IOException {
        writeDoc(results, out);
    }

    /**
     * write the validation report to a stream
     */
    public void writeReport(File out) throws IOException {
        writeDoc(results, out);
    }

    /**
     * write the resource record to a stream
     */
    void writeDoc(Document srcdoc, File out) throws IOException {
        Writer os = new FileWriter(out);
        try {
            writeDoc(srcdoc, os);
        } finally {
            if (os != null) os.close();
        }
    }

    /**
     * write the resource record to a stream
     */
    void writeDoc(Document srcdoc, Writer out) throws IOException {
        prepForWriting();
        try {
            DOMSource source = new DOMSource(srcdoc);
            StreamResult result = new StreamResult(out);
            xmlwrtr.transform(source, result);
        }
        catch (TransformerException ex) {
            throw new IOException("XML output error: "+ex.getMessage(), ex);
        }
    }

    /** 
     * the number of test failures for this record
     */
    public int failCount() {  return ntype[0];  }

    /** 
     * the number of test warnings for this record
     */
    public int warnCount() {  return ntype[1];  }

    /** 
     * the number of test recommendations for this record
     */
    public int recCount()  {  return ntype[2];  }

    /** 
     * the number of test that this record pass completed successfull
     */
    public int passCount() {  return ntype[3];  }

    /** 
     * the total number of tests conducted
     */
    public int testCount() {  return ncount;    }

    /**
     * return an iterator that will iterate 
     */
    public Iterator<Issue> issues() { return new IssueIter((String) null); }
    public Iterator<Issue> failures() { return new IssueIter("fail"); }
    public Iterator<Issue> warnings() { return new IssueIter("warn"); }
    public Iterator<Issue> recommendations() { return new IssueIter("rec");}

    class TestIter implements Iterator<Element> {
        Node nxt = null;
        TestIter() { nxt = resultRoot.getFirstChild();  makeReady();  }
        protected boolean ready() { return "test".equals(nxt.getNodeName()); }
        void setNext() {
            if (nxt != null) 
                nxt = nxt.getNextSibling();  makeReady();
        }
        void makeReady() {
            while (nxt != null && ! ready()) 
                nxt = nxt.getNextSibling();
        }
        public boolean hasNext() { return (nxt != null); }
        public Element next() {
            Element out = (Element) nxt;  setNext();  return out;
        }
        public void remove() {throw new UnsupportedOperationException("remove");}
    }
    class ByStatusIter extends TestIter {
        String want = null;
        ByStatusIter(String status) { super(); want = status; makeReady(); }
        void makeReady() { if (want != null) super.makeReady(); }
        protected boolean ready() { 
            return super.ready() && 
                want.equals(((Element) nxt).getAttribute("status"));
        }
    }
    class IssueIter implements Iterator<Issue> {
        TestIter it = null;
        IssueIter(TestIter elit) { it = elit; }
        IssueIter(String status) {
            this((status==null) ? new TestIter() : new ByStatusIter(status));
        }
        public boolean hasNext() { return it.hasNext(); }
        public Issue next() { 
            Element out = it.next();
            if (out == null) return null;
            return new Issue(out);
        }
        public void remove() {throw new UnsupportedOperationException("remove");}
    }

    /**
     * a representation of an issue raised as part of validation
     */
    public class Issue {
        Element test = null;

        Issue(Element testElement) { test = testElement; }

        /**
         * return the status of the issue
         */
        public String getStatus() { return test.getAttribute("status"); }

        /**
         * return the identifying label for the issue
         */
        public String getLabel() { return test.getAttribute("item"); }

        /**
         * return the description
         */
        public String getDescription() {
            StringBuilder sb = new StringBuilder();
            Node child = test.getFirstChild();
            if (child == null) return "";
            while (child != null) {
                sb.append(child.getNodeValue());
                child = child.getNextSibling();
            }
            return sb.toString().trim();
        }
    }
}