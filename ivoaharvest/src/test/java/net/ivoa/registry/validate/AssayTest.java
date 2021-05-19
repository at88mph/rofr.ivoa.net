package net.ivoa.registry.validate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.File;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.apache.commons.io.FileUtils;

public class AssayTest {

    String testreport = "valreport.xml";
    File tmpdir = new File(System.getProperty("test.tmpdir", "/tmp"));
    Transformer trans = null;
    DocumentBuilder db = null;
    Assay ass = null;

    public AssayTest() {
        try {
            trans = TransformerFactory.newInstance().newTransformer();
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (TransformerException | ParserConfigurationException ex) {
            fail("could not create transformer");
        }
    }

    @Before public void setup() throws IOException, SAXException {
        InputStream res = getClass().getClassLoader().getResourceAsStream(testreport);
        Document doc = db.parse(res);
        ass = new Assay(doc, 40, trans);
    }

    @Test public void testCounts() {
        assertNull(ass.getResourceRecord());
        assertEquals(8, ass.failCount());
        assertEquals(2, ass.warnCount());
        assertEquals(1, ass.recCount());
    }

    @Test public void testTests() {
        Iterator<Assay.Issue> it = ass.issues();
        assertNotNull(it);

        int i = 0;
        Assay.Issue issue = null;
        while (it.hasNext()) {
            issue = it.next();
            assertNotNull(issue);
            i++;
            assertNotNull(issue.getDescription());
            assertNotNull(issue.getLabel());
            assertNotNull(issue.getStatus());
        }

        assertEquals(11, i);
    }

    @Test public void testFailures() {
        Iterator<Assay.Issue> it = ass.failures();
        assertNotNull(it);

        int i = 0;
        Assay.Issue issue = null;
        while (it.hasNext()) {
            issue = it.next();
            assertNotNull(issue);
            i++;
            assertNotNull(issue.getDescription());
            assertNotNull(issue.getLabel());
            assertEquals("fail", issue.getStatus());
        }

        assertEquals(8, i);
    }

    @Test public void testRecommendations() {
        Iterator<Assay.Issue> it = ass.recommendations();
        assertNotNull(it);

        int i = 0;
        Assay.Issue issue = null;
        while (it.hasNext()) {
            issue = it.next();
            assertNotNull(issue);
            i++;
            assertNotNull(issue.getDescription());
            assertNotNull(issue.getLabel());
            assertEquals("rec", issue.getStatus());
        }

        assertEquals(1, i);
    }

    @Test public void testWarnings() {
        Iterator<Assay.Issue> it = ass.warnings();
        assertNotNull(it);

        int i = 0;
        Assay.Issue issue = null;
        while (it.hasNext()) {
            issue = it.next();
            assertNotNull(issue);
            i++;
            assertNotNull(issue.getDescription());
            assertNotNull(issue.getLabel());
            assertEquals("warn", issue.getStatus());
        }

        assertEquals(2, i);
    }

    @Test public void testIssue() {
        Assay.Issue issue = ass.issues().next();
        assertEquals("VRvalid", issue.getLabel());
        assertEquals("fail", issue.getStatus());
        assertTrue(issue.getDescription().startsWith("Resource record must be compliant"));
    }

    @Test public void testWriteReport() throws IOException {
        File out = new File(tmpdir, "valrep-out.xml");
        try {
            ass.writeReport(out);
            assertTrue("failed to write file", out.exists());
            assertEquals("output wrong size", 2394L, FileUtils.sizeOf(out));
        }
        finally {
            if (out.exists()) out.delete();
        }
    }

    public static void main(String[] args) {
        AssayTest test = new AssayTest();
        try {
            test.setup();
            // test.testCounts();
            test.testFailures();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }    
}