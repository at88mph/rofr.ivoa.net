package ncsa.xml.saxfilter;

import java.util.ListIterator;

import static java.lang.String.format;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class TextBufferTest {

    TextBuffer tb = null;

    @Before
    public void setup() {
        tb = new TextBuffer();
    }

    @Test
    public void testCtor() {
        assertEquals(0, tb.size());
        assertEquals(0, tb.deque.size());
    }

    @Test
    public void testAppend() {
        testCtor();
        String s1 = "The quick brown fox ";
        tb.append(s1);
        assertEquals(s1.length(), tb.size());
        assertEquals(s1, tb.getFirst().toString());
        assertEquals(s1, tb.toString());

        String s2 = "jumped over ";
        tb.append(s2);
        assertEquals(s1.length()+s2.length(), tb.size());
        assertEquals(s1, tb.getFirst().toString());
        assertEquals(s2, tb.getLast().toString());
        assertEquals("The quick brown fox jumped over ", tb.toString());

        String s3 = "the lazy dogs.";
        tb.append(s3);
        assertEquals(s1.length()+s2.length()+s3.length(), tb.size());
        assertEquals(s1, tb.getFirst().toString());
        assertEquals(s3, tb.getLast().toString());
        assertEquals("The quick brown fox jumped over the lazy dogs.", 
                     tb.toString());
    }

    @Test
    public void testPopChars() {
        testAppend();
        tb.popChars(4);
        assertEquals("quick brown fox jumped over the lazy dogs.", 
                     tb.toString());
        tb.popChars(20);
        assertEquals("ed over the lazy dogs.", tb.toString());
        assertEquals("ed over ", tb.getFirst().toString());
    }

    @Test
    public void testGetSubstring1() {
        testAppend();
        ListIterator it = tb.getSubstring(27);
        Substring sub = (Substring) it.next();
        assertEquals("over ", sub.toString());
        assertTrue(it.hasNext());
        sub = (Substring) it.next();
        assertEquals("the lazy dogs.", sub.toString());
        assertFalse(it.hasNext());
        assertTrue(it.hasPrevious());
        sub = (Substring) it.previous();
        assertEquals("the lazy dogs.", sub.toString());
        assertTrue(it.hasPrevious());
        sub = (Substring) it.previous();
        assertEquals("over ", sub.toString());
        assertTrue(it.hasPrevious());
        sub = (Substring) it.previous();
        assertEquals("The quick brown fox ", sub.toString());
        assertFalse(it.hasPrevious());
    }

    @Test
    public void testGetSubstring2() {
        testPopChars();
        ListIterator it = tb.getSubstring(3);
        assertFalse(it.hasPrevious());
        Substring sub = (Substring) it.next();
        assertEquals("over ", sub.toString());
        assertTrue(it.hasNext());
        sub = (Substring) it.next();
        assertEquals("the lazy dogs.", sub.toString());
    }

    @Test
    public void testInsert() {
        testAppend();
        tb.insert("yellow and ", 10);
        assertEquals("The quick yellow and brown fox jumped over the lazy dogs.", 
                        tb.toString());

        tb.insert("S: ", 0);
        assertEquals("S: The quick yellow and brown fox jumped over the lazy dogs.", 
                        tb.toString());

        tb.insert("-assed", 54);
        assertEquals("S: The quick yellow and brown fox jumped over the lazy-assed dogs.", 
                        tb.toString());

        tb.insert("!!", tb.toString().length());
        assertEquals("S: The quick yellow and brown fox jumped over the lazy-assed dogs.!!", 
                        tb.toString());

    }

    @Test(expected=StringIndexOutOfBoundsException.class) 
    public void testBadInsert() {
        testAppend();
        tb.insert("!!", 400);
    }

    @Test(expected=StringIndexOutOfBoundsException.class) 
    public void testBadNegInsert() {
        testAppend();
        tb.insert("!!", -4);
    }

    @Test
    public void testCopy() {
        testAppend();
        char[] buf = new char[tb.length];
        for(int i=0; i < buf.length; i++) buf[i] = '|';

        tb.copy(0, buf, 0, 3);
        assertEquals("The|", new String(buf, 0, 4));
        tb.copy(4, buf, 4, 5);
        assertEquals("The|quick|", new String(buf, 0, 10));
        tb.copy(10, buf, 0, 16);
        assertEquals("brown fox jumped|", new String(buf, 0, 17));
    }

    @Test(expected=ArrayIndexOutOfBoundsException.class)
    public void testBadCopy() {
        testAppend();
        char[] buf = new char[tb.length];
        tb.copy(400, buf, 0, 4);
    }

    @Test(expected=ArrayIndexOutOfBoundsException.class)
    public void testBadNegCopy() {
        testAppend();
        char[] buf = new char[tb.length];
        tb.copy(-4, buf, 0, 4);
    }

    @Test
    public void testSubstitute1() {
        testAppend();
        tb.substitute("yellow", 10, 5);
        assertEquals("The quick yellow fox ", tb.getFirst().toString());
        tb.substitute("red", 10, 6);
        assertEquals("The quick red fox ", tb.getFirst().toString());
        tb.substitute("foxes", 14, 4);
        assertEquals("The quick red foxes", tb.getFirst().toString());

        tb.substitute("fox leap", 14, 9);
        assertEquals("The quick red fox leap", tb.getFirst().toString());
        assertTrue(tb.toString().startsWith("The quick red fox leaped over"));

        tb.substitute("", 24, tb.size()-24);
        assertEquals("The quick red fox leap", tb.getFirst().toString());
        assertEquals("The quick red fox leaped", tb.toString());
        assertEquals(24, tb.size());

        // len goes past end is okay
        tb.substitute(".", 17, 40);
        assertEquals("The quick red fox.", tb.getFirst().toString());
        assertEquals("The quick red fox.", tb.toString());
        assertEquals(18, tb.size());

        // appending with substitute
        tb.substitute("..", 18, 8);
        assertEquals("The quick red fox.", tb.getFirst().toString());
        assertEquals("The quick red fox...", tb.toString());
        assertEquals(20, tb.size());
    }

    @Test
    public void testSubstitute2() {
        testAppend();
        tb.substitute("red", 10, 5);
        assertEquals("The quick red fox ", tb.getFirst().toString());
        tb.substitute("yellow", 10, 3);
        assertEquals("The quick yellow fox ", tb.getFirst().toString());
    }

    @Test
    public void testSubstitute3() {
        testAppend();
        tb.popChars(4);
        tb.substitute("red", 6, 5);
        assertEquals("quick red fox ", tb.getFirst().toString());
        tb.substitute("yellow", 6, 3);
        assertEquals("quick yellow fox ", tb.getFirst().toString());
    }

    public static void main(String[] args) {
        TextBufferTest tbt = new TextBufferTest();
        tbt.setup();
        tbt.testSubstitute1();
    }
}