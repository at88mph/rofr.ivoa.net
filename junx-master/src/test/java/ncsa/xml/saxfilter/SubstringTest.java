package ncsa.xml.saxfilter;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class SubstringTest {

    String test = "The quick brown fox";
    Substring sub = null;

    @Test
    public void testStrCtor() {
        sub = new Substring(test);
        assertEquals(0, sub.off);
        assertEquals(test.length(), sub.len);
        assertEquals(test, sub.str());
        assertEquals(test, sub.toString());
    }

    @Test
    public void testCtorStrOff() {
        sub = new Substring(test, 4, 5);
        assertEquals(4, sub.off);
        assertEquals(5, sub.len);
        assertEquals(test, sub.str());
        assertEquals("quick", sub.toString());
    }

    @Test
    public void testCtorChar() {
        sub = new Substring(test.toCharArray(), 10, 5);
        assertEquals(10, sub.off);
        assertEquals(5, sub.len);
        assertEquals(test, sub.str());
        assertEquals("brown", sub.toString());
    }

    @Test
    public void testCtorTooFar() {
        sub = new Substring(test, 16, 10);
        assertEquals(16, sub.off);
        assertEquals(10, sub.len);
        assertEquals(test, sub.str());
        assertEquals("fox", sub.toString());
    }

}

