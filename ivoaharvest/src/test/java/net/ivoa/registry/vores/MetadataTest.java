package net.ivoa.registry.vores;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataTest {
    static String tstres = "res.xml";

    DocumentBuilder db = null;
    Element res = null;
    Metadata md = null;

    public MetadataTest() {
        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            fact.setNamespaceAware(true);
            db = fact.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            fail("could not create transformer");
        }
    }

    @Before public void setup() throws IOException, SAXException {
        InputStream rs = getClass().getClassLoader().getResourceAsStream(tstres);
        Document doc = db.parse(rs);
        res = doc.getDocumentElement();
        md = new Metadata(res);
    }

    @Test public void testPathName() {
        assertEquals("ri:Resource", md.getPathName());
        md = new Metadata(res, "goob");
        assertEquals("goob", md.getPathName());
    }

    @Test public void testCtor() {
        assertSame(res, md.getDOMNode());
    }

    @Test public void testParameter() {
        assertEquals("VLACOSMOS", md.getParameter("shortName"));
        String[] vals = md.getParameters("shortName");
        assertEquals(1, vals.length);
        assertEquals("VLACOSMOS", vals[0]);

        assertEquals("LEDAS", md.getParameter("curation/creator/name"));

        assertEquals("LEDAS", md.getParameter("content/subject"));
        assertNull(md.getParameter("content/@subject"));
        vals = md.getParameters("content/subject");
        assertEquals(2, vals.length);
        assertEquals("LEDAS", vals[0]);
        assertEquals("vlacosmos", vals[1]);
        vals = md.getParameters("content/@subject");
        assertEquals(0, vals.length);

        assertEquals("active", md.getParameter("status"));
        assertEquals("active", md.getParameter("@status"));

        vals = md.getParameters("capability/standardID");
        assertEquals(2, vals.length);
        vals = md.getParameters("capability/@standardID");
        assertEquals(2, vals.length);

        try {
            vals = md.getParameters("capability/@standardID/interface");
            fail("Failed to detect bad path syntax");
        } catch (IllegalArgumentException ex) {
            // Yeah!
        } catch (Exception ex) {
            fail(" bad path syntax trips unexpected exception: " + 
                 ex.getClass().getName());
        }
    }

    @Test public void testBlocks() {
        Metadata[] subs = md.getBlocks("content");
        assertEquals(1, subs.length);
        assertEquals("Catalog", subs[0].getParameter("type"));

        subs = md.getBlocks("capability");
        assertEquals(2, subs.length);
        assertEquals("base", subs[0].getParameter("interface/accessURL/use"));
        assertEquals("full", subs[1].getParameter("interface/accessURL/use"));

        subs = md.getBlocks("capability/interface");
        assertEquals(3, subs.length);
        assertEquals("base", subs[0].getParameter("accessURL/use"));
        assertEquals("full", subs[1].getParameter("accessURL/use"));
        assertEquals("full", subs[2].getParameter("accessURL/use"));

        subs = md.getBlocks("tableset/schema/table/column");
        assertEquals(18, subs.length);
    }

    @Test public void testXSIType() {
        assertEquals("CatalogService", md.getXSIType());
    }

    @Test public void testClearCache() {
        assertEquals("active", md.getParameter("status"));
        res.setAttribute("status", "deleted");
        assertEquals("active", md.getParameter("status"));
        md.clearCache();
        assertEquals("deleted", md.getParameter("status"));
    }

    @Test public void testValLev() {
        Map<String, Integer> lu = Metadata.getValidationLevels(md);
        assertEquals(1, lu.size());
        assertEquals(1, lu.get("ivo://archive.stsci.edu").intValue());

        assertEquals(1, Metadata.getValidationLevelBy(md, 
                                                      "ivo://archive.stsci.edu")
                                .intValue());
        assertNull(Metadata.getValidationLevelBy(md, "ivo://nvo.ncsa/registry"));
    }

    public static void main(String[] args) {
        MetadataTest test = new MetadataTest();
        try {
            test.setup();
            test.testXSIType();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}