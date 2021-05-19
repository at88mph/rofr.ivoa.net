package ncsa.xml.sax;

import ncsa.xml.sax.EntityExpander;

import static java.lang.String.format;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntityExpanderTest {

    public static final String textin = "&lt;vao id=&quot;&go&gb;ob&qq;&gt;";
    public static final String textout = "<vao id=\"&go&gb;ob\">";

    @Test public void testDefaultConstructor() {
        EntityExpander ee = new EntityExpander();
        assertEquals("bad def: amp:", "&",  ee.getExpansion("amp"));
        assertEquals("bad def: lt:",  "<",  ee.getExpansion("lt") );
        assertEquals("bad def: gt:",  ">",  ee.getExpansion("gt") );
        assertEquals("bad def: quot:","\"", ee.getExpansion("quot"));
        assertEquals("bad def: apos:","'",  ee.getExpansion("apos"));
    }

    @Test public void testRedefine() {
        EntityExpander ee = new EntityExpander();
        assertFalse("redefine does not default to false", ee.redefineAllowed());

        assertFalse("redefine incorrectly allowed", ee.define("amp", "but"));
        assertEquals("amp redefined:", "&",  ee.getExpansion("amp"));

        ee.allowRedefine(true);
        assertTrue("redefine not enabled", ee.redefineAllowed());

        assertTrue("redefine incorrectly disallowed", ee.define("amp", "but"));
        assertEquals("amp not redefined:", "but",  ee.getExpansion("amp"));

        ee.allowRedefine(false);
        assertFalse("redefine not disabled", ee.redefineAllowed());

        assertFalse("redefine incorrectly allowed", ee.define("amp", "#"));
        assertEquals("amp incorrectly redefined:", 
                     "but",  ee.getExpansion("amp"));
    }

    @Test public void testDefine() {
        EntityExpander ee = new EntityExpander();
        assertNull("value for undefined entity", ee.getExpansion("tab"));
        ee.define("tab", "\t");
        assertEquals("bad def: tab:",  "\t",  ee.getExpansion("tab") );
    }

    @Test public void testExpand() {
        EntityExpander ee = new EntityExpander();
        ee.define("qq", "\"");
        assertEquals("failed expansion:", textout, ee.expand(textin));
    }
}

