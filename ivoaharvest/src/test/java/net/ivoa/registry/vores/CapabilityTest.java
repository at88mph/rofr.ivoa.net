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

public class CapabilityTest {
    static String tstres = "res.xml";

    DocumentBuilder db = null;
    Element res = null;
    VOResource vor = null;
    Capability cap = null;

    public CapabilityTest() {
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

    @Test public void testGetCapInfo() {
        cap = vor.findCapabilityByID("ivo://ivoa.net/std/ConeSearch");
        assertEquals("ConeSearch", cap.getCapabilityClass());
        assertEquals("ivo://ivoa.net/std/ConeSearch", cap.getStandardID());
        cap = vor.findCapabilityByID("ivo://ivoa.net/std/VOSI#availability");
        assertEquals("ivo://ivoa.net/std/VOSI#availability", 
                     cap.getStandardID());
        assertEquals("Capability", cap.getCapabilityClass());
    }

    @Test public void testValLev() {
        cap = vor.findCapabilityByID("ivo://ivoa.net/std/ConeSearch");
        Map<String, Integer> lu = cap.getValidationLevels();
        assertEquals(1, lu.size());
        assertEquals(2, lu.get("ivo://archive.stsci.edu").intValue());

        assertEquals(2, cap.getValidationLevelBy("ivo://archive.stsci.edu")
                           .intValue());
        assertNull(cap.getValidationLevelBy("ivo://nvo.ncsa/registry"));
    }

    @Test public void testGetInterface() {
        Metadata intf = null;
        cap = vor.findCapabilityByID("ivo://ivoa.net/std/ConeSearch");

        // find std CS interface; be strict
        intf = cap.getInterface("std", "1.0", false);
        assertNotNull(intf);
        assertEquals("ParamHTTP", intf.getXSIType());
        assertEquals("base", intf.getParameter("accessURL/use"));

        // find std CS interface; be lenient (result is the same)
        intf = cap.getInterface("std", "1.0", true);
        assertNotNull(intf);
        assertEquals("ParamHTTP", intf.getXSIType());
        assertEquals("base", intf.getParameter("accessURL/use"));

        // find std CS interface; be strict (result is the same)
        intf = cap.getInterface("std", "1.0");
        assertNotNull(intf);
        assertEquals("ParamHTTP", intf.getXSIType());
        assertEquals("base", intf.getParameter("accessURL/use"));

        // find web page CS interface; be strict
        intf = cap.getInterface("", "1.0", false);
        assertNotNull(intf);
        assertEquals("WebBrowser", intf.getXSIType());
        assertEquals("full", intf.getParameter("accessURL/use"));

        // find web page CS interface; be strict
        intf = cap.getInterface(null, "1.0", false);
        assertNotNull(intf);
        assertEquals("WebBrowser", intf.getXSIType());
        assertEquals("full", intf.getParameter("accessURL/use"));

        // find web page CS interface; be lenient
        intf = cap.getInterface("", "1.0", true);
        assertNotNull(intf);
        assertEquals("WebBrowser", intf.getXSIType());
        assertEquals("full", intf.getParameter("accessURL/use"));

        intf = cap.getInterface(null, null, true);
        assertNull(intf);

        cap = vor.findCapabilityByID("ivo://ivoa.net/std/VOSI#availability");
        intf = cap.getInterface("std", "1.0", false);
        assertNull(intf);

        // test leniency
        intf = cap.getInterface("std", "1.0", true);
        assertNotNull(intf);
        assertEquals("ParamHTTP", intf.getXSIType());
        assertEquals("full", intf.getParameter("accessURL/use"));
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/ledas/vosi/availability", intf.getParameter("accessURL"));
    }

    @Test public void testGetStdInterface() {
        Metadata intf = null;
        cap = vor.findCapabilityByID("ivo://ivoa.net/std/ConeSearch");

        // metadata is fully labeled; be strict
        intf = cap.getStandardInterface("1.0", false);
        assertNotNull(intf);
        assertEquals("ParamHTTP", intf.getXSIType());
        assertEquals("base", intf.getParameter("accessURL/use"));

        // metadata is fully labeled; be strict (same result)
        intf = cap.getStandardInterface(null, false);
        assertNotNull(intf);
        assertEquals("ParamHTTP", intf.getXSIType());
        assertEquals("base", intf.getParameter("accessURL/use"));

        // metadata is fully labeled; be lenient (same result)
        intf = cap.getStandardInterface("1.0", true);
        assertNotNull(intf);
        assertEquals("ParamHTTP", intf.getXSIType());
        assertEquals("base", intf.getParameter("accessURL/use"));

        // metadata is laxly labeled; be strict
        cap = vor.findCapabilityByID("ivo://ivoa.net/std/VOSI#availability");
        intf = cap.getStandardInterface("1.0", false);
        assertNull(intf);

        // metadata is laxly labeled; be lenient
        intf = cap.getStandardInterface("1.0", true);
        assertNotNull(intf);
        assertEquals("ParamHTTP", intf.getXSIType());
        assertEquals("full", intf.getParameter("accessURL/use"));
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/ledas/vosi/availability", intf.getParameter("accessURL"));

        // metadata is laxly labeled; be lenient
        intf = cap.getStandardInterface(null, true);
        assertNotNull(intf);
        assertEquals("ParamHTTP", intf.getXSIType());
        assertEquals("full", intf.getParameter("accessURL/use"));
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/ledas/vosi/availability", intf.getParameter("accessURL"));
    }

    @Test public void testGetAccessURL() {
        cap = vor.findCapabilityByID("ivo://ivoa.net/std/ConeSearch");
        String url = null;

        // find std CS interface; be strict
        url = cap.getAccessURL("std", "1.0", false);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone?DSACAT=ledas&DSATAB=vlacosmos&", url);

        // find std CS interface; be lenient (result is the same)
        url = cap.getAccessURL("std", "1.0", true);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone?DSACAT=ledas&DSATAB=vlacosmos&", url);

        // find std CS interface; be strict (result is the same)
        url = cap.getAccessURL("std", "1.0");
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone?DSACAT=ledas&DSATAB=vlacosmos&", url);

        // find web page CS interface; be strict
        url = cap.getAccessURL("", "1.0", false);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone.jsp?DSACAT=ledas&DSATAB=vlacosmos&", url);

        // find web page CS interface; be strict
        url = cap.getAccessURL(null, "1.0", false);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone.jsp?DSACAT=ledas&DSATAB=vlacosmos&", url);

        // find web page CS interface; be lenient
        url = cap.getAccessURL("", "1.0", true);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone.jsp?DSACAT=ledas&DSATAB=vlacosmos&", url);

        url = cap.getAccessURL(null, null, true);
        assertNull(url);

        cap = vor.findCapabilityByID("ivo://ivoa.net/std/VOSI#availability");
        url = cap.getAccessURL("std", "1.0", false);
        assertNull(url);

        // test leniency
        url = cap.getAccessURL("std", "1.0", true);
        assertNotNull(url);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/ledas/vosi/availability", url);

    }

    @Test public void testGetStdAccessURL() {
        cap = vor.findCapabilityByID("ivo://ivoa.net/std/ConeSearch");
        String url = null;

        // find std CS interface; be strict
        url = cap.getStandardAccessURL("1.0", false);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone?DSACAT=ledas&DSATAB=vlacosmos&", url);

        // find std CS interface; be lenient (result is the same)
        url = cap.getStandardAccessURL("1.0", true);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone?DSACAT=ledas&DSATAB=vlacosmos&", url);

        // metadata is fully labeled; be strict
        url = cap.getStandardAccessURL(null, false);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone?DSACAT=ledas&DSATAB=vlacosmos&", url);

        url = cap.getStandardAccessURL(null, true);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/SubmitCone?DSACAT=ledas&DSATAB=vlacosmos&", url);

        cap = vor.findCapabilityByID("ivo://ivoa.net/std/VOSI#availability");
        url = cap.getStandardAccessURL("1.0", false);
        assertNull(url);

        // test leniency
        url = cap.getStandardAccessURL("1.0", true);
        assertNotNull(url);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/ledas/vosi/availability", url);

        url = cap.getStandardAccessURL(null, true);
        assertNotNull(url);
        assertEquals("http://camelot.star.le.ac.uk:8080/dsa-catalog/ledas/vosi/availability", url);

    }

    public static void main(String[] args) {
        CapabilityTest test = new CapabilityTest();
        try {
            test.setup();
            // test.testGetCapInfo();
            test.testGetAccessURL();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

}