package net.ivoa.registry.util;

import java.io.Reader;
import java.io.FileReader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResourceUpdaterTest {
    static final String tstrec = "vores.xml";
    static final String tstupd = "vores-upd.xml";
    static final String tstupd2 = "vores-upd2.xml";
    static final String tmppath = System.getProperty("test.tmpdir", "/tmp");
    static final File tmpdir = new File(tmppath);

    ResourceUpdater updr = null;

    @Before
    public void setup() {
        updr = new ResourceUpdater();
    }

    Reader openResource(String resname) {  
        return new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resname)));
    }
    File getOutFile() throws IOException {
        File out = File.createTempFile("upd", ".xml", tmpdir);
        out.deleteOnExit();
        return out;
    }

    @Test
    public void testAlmostEquals() throws IOException {
        Reader is = openResource(tstrec);
        String src = IOUtils.toString(is);
        is.close();
        is = openResource(tstupd);
        String upd = IOUtils.toString(is);
        is.close();

        String[] atts = { "xmlns", "status", "updated" };
        assertTrue("almostEquals failed", almostEqual(src, upd, atts));

        is = openResource(tstupd2);
        upd = IOUtils.toString(is);
        is.close();

        assertFalse("almostEquals failed", almostEqual(src, upd, atts));
    }

    @Test
    public void testUpdateRoot() throws IOException {
        File out = getOutFile();
        FileWriter os = new FileWriter(out);
        Reader is = openResource(tstrec);
        updr.update(is, os);
        os.close();
        is.close();

        is = openResource(tstrec);
        String source = IOUtils.toString(is);
        is.close();
        String result = FileUtils.readFileToString(out);

        assertFalse("data not updated", source.equals(result));
        assertUpdatedAtt(source, result, "updated");

        String[] atts = { "updated" };
        assertTrue("more than updated updated", 
                   almostEqual(source, result, atts));
    }

    @Test
    public void testUpdateElem() throws IOException {
        updr.setResourceRoot("", "resource");

        File out = getOutFile();
        FileWriter os = new FileWriter(out);
        Reader is = openResource(tstrec);
        updr.update(is, os);
        os.close();
        is.close();

        is = openResource(tstrec);
        String source = IOUtils.toString(is);
        is.close();
        String result = FileUtils.readFileToString(out);

        assertFalse("data not updated", source.equals(result));
        assertUpdatedAtt(source, result, "updated");

        String[] atts = { "updated" };
        assertTrue("more than updated updated", 
                   almostEqual(source, result, atts));
    }

    @Test
    public void testUpdateBadElem() throws IOException {
        updr.setResourceRoot("urn:goob", "resource");

        File out = getOutFile();
        FileWriter os = new FileWriter(out);
        Reader is = openResource(tstrec);
        updr.update(is, os);
        os.close();
        is.close();

        is = openResource(tstrec);
        String source = IOUtils.toString(is);
        is.close();
        String result = FileUtils.readFileToString(out);

        assertTrue("data got updated (when it shouldn't've)", 
                   source.equals(result));
    }

    @Test
    public void testUpdateStatus() throws IOException {
        File out = getOutFile();
        FileWriter os = new FileWriter(out);
        Reader is = openResource(tstrec);
        updr.setStatus(ResourceUpdater.Status.DELETED);
        updr.update(is, os);
        os.close();
        is.close();

        is = openResource(tstrec);
        String source = IOUtils.toString(is);
        is.close();
        String result = FileUtils.readFileToString(out);

        assertFalse("data not updated", source.equals(result));
        assertUpdatedAtt(source, result, "updated");
        assertUpdatedAtt(source, result, "status");

        String[] atts = { "updated", "status" };
        assertTrue("more than updated/status updated", 
                   almostEqual(source, result, atts));
    }

    @Test
    public void testUpdateDefNS() throws IOException {
        File out = getOutFile();
        FileWriter os = new FileWriter(out);
        Reader is = openResource(tstrec);
        updr.setDefNamespace();
        updr.update(is, os);
        os.close();
        is.close();

        is = openResource(tstrec);
        String source = IOUtils.toString(is);
        is.close();
        String result = FileUtils.readFileToString(out);

        assertFalse("data not updated", source.equals(result));
        assertEquals("", getAttributeValue(result, "xmlns"));
        assertNull(getAttributeValue(source, "xmlns"));
        assertUpdatedAtt(source, result, "updated");

        String[] atts = { "updated", "xmlns" };
        assertTrue("more than updated/xmlns updated", 
                   almostEqual(source, result, atts));
    }

    @Test
    public void testFormatMyDate() {
        Calendar mycal = new GregorianCalendar();
        mycal.set(2013, 3, 3, 10, 15, 8);
        String date = updr.getDateFormat().format(mycal.getTime());
        assertEquals("2013-04-03T10:15:08Z", date);
    }

    @Test
    public void testUpdateMyDate() throws IOException {
        Calendar mycal = new GregorianCalendar();
        mycal.set(2013, 3, 3, 10, 15, 8);

        updr.setUpdateDate(mycal.getTime());

        File out = getOutFile();
        FileWriter os = new FileWriter(out);
        Reader is = openResource(tstrec);
        updr.update(is, os);
        os.close();
        is.close();

        is = openResource(tstrec);
        String source = IOUtils.toString(is);
        is.close();
        String result = FileUtils.readFileToString(out);

        assertFalse("data not updated", source.equals(result));
        assertUpdatedAtt(source, result, "updated");
        assertEquals("2013-04-03T10:15:08Z", 
                     getAttributeValue(result,"updated"));

        String[] atts = { "updated" };
        assertTrue("more than updated updated", 
                   almostEqual(source, result, atts));
    }

    @Test
    public void testUpdateCreated() throws IOException {
        Calendar mycal = new GregorianCalendar();
        mycal.set(2013, 3, 3, 10, 15, 8);

        updr.setUpdateDate(mycal.getTime());
        updr.setCreatedDate(true);

        File out = getOutFile();
        FileWriter os = new FileWriter(out);
        Reader is = openResource(tstrec);
        updr.update(is, os);
        os.close();
        is.close();

        is = openResource(tstrec);
        String source = IOUtils.toString(is);
        is.close();
        String result = FileUtils.readFileToString(out);

        assertFalse("data not updated", source.equals(result));
        assertUpdatedAtt(source, result, "updated");
        assertUpdatedAtt(source, result, "created");
        assertEquals("2013-04-03T10:15:08Z", 
                     getAttributeValue(result,"updated"));
        assertEquals("2013-04-03T10:15:08Z", 
                     getAttributeValue(result,"created"));

        String[] atts = { "updated", "created" };
        assertTrue("more than updated/created updated", 
                   almostEqual(source, result, atts));
    }

    public boolean almostEqual(String s1, String s2, String[] attnames) {
        if (attnames.length == 0) 
            return s1.equals(s2);

        StringBuilder sb = new StringBuilder("(");
        for(String name : attnames) 
            sb.append(name).append('|');
        sb.deleteCharAt(sb.length()-1);
        sb.append(")=");

        Pattern atts = Pattern.compile(sb.toString());
        Matcher m2 = atts.matcher(s2);
        int c2 = 0, p = 0;
        String sub = null;
        char q = '"';

        // s1 is the source data, s2 is the updated data, c2 is a cursor 
        // into s2.  We expect s2 to look just like s1 except either (1)
        // some attributes (given in attnames) will have different values, 
        // or (2) s2 will have additional attributes (given in attnames) 
        // not found in s1.
        //
        // As we loop we are comparing portions of the texts from start to 
        // end.  As we finish comparing a portion, s1 will have the processed 
        // parts from the beginning chopped off (that is, the start of s1
        // is our comparison position), and c2 will be advanced to the current
        // comparison position.  
        // 
        while (m2.find(c2)) {
            // compare text preceding found attribute
            sub = s2.substring(c2, m2.start());
            if (! sub.equals(s1.substring(0, sub.length()))) return false;
            c2 = m2.start();

            // note the quote character used
            q = s2.charAt(m2.end());
            assertTrue("No or bad attribute quote used at: " + 
                       s2.substring(m2.end(), m2.end()+5) + "...", 
                       q == '"' || q == '\'');

            // does the source include the found attribute?
            s1 = s1.substring(sub.length());
            sub = m2.group();
            if (s1.startsWith(m2.group())) {
                // yes
                s1 = s1.substring(sub.length());

                // using the same quote char?
                if (s1.charAt(0) != q) return false;

                p = s1.indexOf(Character.toString(q), 1);
                assertTrue("bad in attribute end quote at: " + 
                           s1.substring(0, 10) + "...", p > 0);
                p++;
                while (Character.isWhitespace(s1.charAt(p))) { p++; }
                s1 = s1.substring(p);
            }

            c2 = m2.end() + 1;  // start of attribute value
            p = s2.indexOf(Character.toString(q), c2);
            assertTrue("bad out attribute end quote at: "+
                       s2.substring(m2.end(), m2.end()+10)+"...", p >= c2);
            c2 = p + 1; // just after ending quote
            while (Character.isWhitespace(s2.charAt(c2))) { c2++; }
        }

        // compare what's left over
        if (! s1.equals(s2.substring(c2))) return false;

        return true;
    }

    public String getAttributeValue(String xml, String attname) {
        int s = xml.indexOf(attname+"=");
        if (s < 0) return null;
        s += attname.length()+1;
        char q = xml.charAt(s++);
        int e = xml.indexOf(Character.toString(q), s);
        if (e < 0) e = xml.length();
        return xml.substring(s,e);
    }

    public void assertUpdatedAtt(String xml1, String xml2, String attname) {
        xml1 = getAttributeValue(xml1, attname);
        xml2 = getAttributeValue(xml2, attname);
        if (xml1 == null && xml2 == null)
            fail("Attribute " + attname + " not added");
        if (xml2 == null)
            fail("Attribute " + attname + " removed from result");
        if (xml1.equals(xml2)) {
            fail("Attribute " + attname + " not updated: <" + xml1 + "> = <" +
                 xml2 + ">");
        }
    }


    public static void main(String[] args) {
        ResourceUpdaterTest test = new ResourceUpdaterTest();
        try {
            test.setup();
            // test.testAlmostEquals();
            // test.testUpdateStatus();
            test.testUpdateDefNS();
        }
        catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}