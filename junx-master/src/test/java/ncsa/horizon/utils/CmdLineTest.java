package ncsa.horizon.utils;

import ncsa.horizon.util.CmdLine;

import java.util.Enumeration;
import java.util.Stack;
import static java.lang.String.format;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class CmdLineTest {

    private static String config = "f:puqx-";
    private static String opts = "fpuqx";
    private static String[] args = "-quq -q -f file.txt -x -hello world".split(" ");
    private CmdLine cl = null;

    @Before
    public void setup() {
        cl = new CmdLine(config);
    }

    @Test 
    public void testGetConfig() {
        assertEquals(cl.getConfig(), config);
    }

    @Test 
    public void testSetConfig() {
        cl.setConfig("re");
        assertFalse(cl.isAnOption(opts.charAt(0)));
        assertTrue(cl.isAnOption('r'));
    }

    @Test public void testSetFlags() {
        assertEquals(cl.getFlags(), CmdLine.NULLFLAG);

        cl.setFlags(CmdLine.RELAX|CmdLine.WARN);
        assertEquals(cl.getFlags(), (CmdLine.RELAX|CmdLine.WARN));
        cl.addFlags(CmdLine.USRWARN);
        assertEquals(cl.getFlags(), 
                     (CmdLine.RELAX|CmdLine.WARN|CmdLine.USRWARN));

        cl.setFlags(CmdLine.NULLFLAG);
        assertEquals(cl.getFlags(), CmdLine.NULLFLAG);
    }

    @Test public void testOptionsDef() {
        StringBuffer set = new StringBuffer();
        for(Enumeration e=cl.options(); e.hasMoreElements();) {
            Character opt = (Character) e.nextElement();
            assertTrue(format("unexpected option: %c", opt), 
                       opts.indexOf(opt) >= 0);
            set.append(opt);
        }
        String optset = set.toString();
        for(int i=0; i < optset.length(); i++) {
            Character opt = optset.charAt(i);
            assertTrue(format("option not set: %c", opt), 
                       opts.indexOf(opt) >= 0);
            assertTrue(format("option not set: %c", opt), 
                       cl.isAnOption(opt));
        }
        assertEquals(optset.length(), opts.length());

        assertTrue("Misconfigured option: p", cl.isSwitched('p'));
        assertTrue("Misconfigured option: u", cl.isSwitched('u'));
        assertTrue("Misconfigured option: q", cl.isSwitched('q'));
        assertTrue("Misconfigured option: x", cl.isSwitched('x'));
        assertFalse("Misconfigured option: f", cl.isSwitched('f'));
    }

    @Test(expected=IllegalStateException.class) 
    public void testNoArgsGetNumArgs() {
        int c = cl.getNumArgs();
        assertFalse("Non-zero arg count given on no-arg", c == 0);
        fail("getNumArgs() failed to raise exception on no-arg");
    }

    @Test
    public void testNoArgsGetNumSet() {
        assertEquals(cl.getNumSet('f'), 0);
    }

    @Test
    public void testNoArgsGetValue() {
        assertNull(cl.getValue('f'));
    }

    @Test
    public void testNoArgsIsSet() {
        assertFalse(cl.isSet('q'));
        assertFalse(cl.isSet('f'));
        assertFalse(cl.isSet('x'));
    }

    @Test
    public void testNoArgsGetAllValues() {
        Stack vals = cl.getAllValues('f');
        assertEquals("Non-empty list of option values on no-arg", 
                     vals.size(), 0);
    }

    @Test public void testEmptyCmdLine() {
        String args[] = new String[0];
        try {
            cl.setCmdLine(args);
        } catch (CmdLine.UnrecognizedOptionException ex) { 
            fail("inadvertantly threw UnrecognizedOptionException");
        }

        for(int i=0; i < opts.length(); i++) {
            assertFalse(cl.isSet(opts.charAt(i)));
        }
        assertEquals(0, cl.getNumArgs());
    }

    @Test public void testBadOption() {
        String args[] = "-re".split(" ");
        try {
            cl.setCmdLine(args);
            fail("Failed to detected unrecognized option");
        } catch (CmdLine.UnrecognizedOptionException ex) { }

        cl.setFlags(CmdLine.RELAX);
        try {
            cl.setCmdLine(args);
        } catch (CmdLine.UnrecognizedOptionException ex) { 
            fail("failed to relax");
        }

        cl.addFlags(CmdLine.USRWARN);
        try {
            cl.setCmdLine(args);
        } catch (CmdLine.UnrecognizedOptionException ex) { 
            fail("failed to relax");
        }
    }

    private void setCmdLine() {
        cl.setFlags(CmdLine.RELAX);
        try {
            cl.setCmdLine(args);
        } catch (CmdLine.UnrecognizedOptionException ex) { 
            fail("failed to relax");
        }
    }

    @Test public void testSetCmdLine() {
        setCmdLine();

        assertEquals("Wrong number of arguments detected", 
                     cl.getNumArgs(), 2);
        assertEquals("Wrong number of arguments detected", 
                     cl.getNumArgs(), 2);
    }

    @Test public void testOptions() {
        setCmdLine();

        assertTrue(cl.isSet('q'));
        assertTrue(cl.isSet('u'));
        assertTrue(cl.isSet('x'));
        assertFalse(cl.isSet('p'));
        assertFalse(cl.isSet('e'));
        assertFalse(cl.isSet('r'));

        assertEquals("Bad option occurance count", cl.getNumSet('q'), 3);

        assertEquals("Bad option arg", cl.getValue('f'), "file.txt");
    }

    @Test public void testArguments() {
        String[] expected = "-hello world".split(" ");

        setCmdLine();
        int i=0;
        StringBuffer extra = new StringBuffer();
        for(Enumeration e = cl.arguments(); e.hasMoreElements(); i++){
            String s = (String) e.nextElement();
            if (i < expected.length)          
                assertEquals(format("Argument out of order: %s", s),
                             s, expected[i]);
            else
                extra.append(' ').append(s);
        }
        assertTrue(format("extra arguments found:%s",extra), 
                   i <= expected.length);
        if (i < expected.length) {
            for(; i < expected.length; i++) 
                extra.append(' ').append(expected[i]);
            fail(format("missing arguments:%s",extra));
        }
    }
}
    
