package net.ivoa.registry.vores;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import java.util.Map;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class VOResourceTest {
    static String tstres = "res.xml";

    DocumentBuilder db = null;
    Element res = null;
    VOResource vor = null;

    public VOResourceTest() {
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
        vor = new VOResource(res);
    }

    @Test public void testIdData() {
        assertEquals("ivo://uk.ac.le.star.tmpledas/ledas/ledas/vlacosmos", 
                     vor.getIdentifier());
        assertEquals("VLACOSMOS: VLA-COSMOS Large Project 1.4-GHz Source Catalog (LEDAS)", 
                     vor.getTitle());
        assertEquals("VLACOSMOS", vor.getShortName());
        assertEquals("CatalogService", vor.getResourceClass());
    }

    @Test public void testValLev() {
        Map<String, Integer> lu = vor.getValidationLevels();
        assertEquals(1, lu.size());
        assertEquals(1, lu.get("ivo://archive.stsci.edu").intValue());

        assertEquals(1, vor.getValidationLevelBy("ivo://archive.stsci.edu")
                           .intValue());
        assertNull(vor.getValidationLevelBy("ivo://nvo.ncsa/registry"));
    }

    @Test public void testCapByType() {
        Capability cap = vor.findCapabilityByType("ConeSearch");
        assertNotNull(cap);
        assertEquals("ConeSearch", cap.getXSIType());

        cap = vor.findCapabilityByType("TableAccess");
        assertNull(cap);
    }

    @Test public void testCapByID() {
        Capability cap = vor.findCapabilityByID("ivo://ivoa.net/std/ConeSearch");
        assertNotNull(cap);
        assertEquals("ConeSearch", cap.getXSIType());
        assertEquals("ivo://ivoa.net/std/ConeSearch", 
                     cap.getParameter("standardID"));

        cap = vor.findCapabilityByID("ivo://ivoa.net/std/VOSI#availability");
        assertNotNull(cap);
        assertEquals("ivo://ivoa.net/std/VOSI#availability", 
                     cap.getParameter("standardID"));

        cap = vor.findCapabilityByType("ConeSearch");
        assertNotNull(cap);
        assertEquals("ConeSearch", cap.getXSIType());
        assertEquals("ivo://ivoa.net/std/ConeSearch", 
                     cap.getParameter("standardID"));
    }

    public static void main(String[] args) {
        VOResourceTest test = new VOResourceTest();
        try {
            test.setup();
            test.testIdData();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

}
