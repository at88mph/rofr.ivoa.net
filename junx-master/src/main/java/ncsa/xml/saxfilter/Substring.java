/*
 * NCSA BIMAgrid
 * Radio Astronomy Imaging Group
 * National Center for Supercomputing Applications
 * University of Illinois at Urbana-Champaign
 * 605 E. Springfield, Champaign IL 61820
 * rai@ncsa.uiuc.edu
 *
 *-------------------------------------------------------------------------
 * History: 
 *  2001/07/26  rlp  initial version
 */
package ncsa.xml.saxfilter;

public class Substring {
    String string = null;

    /**
     * the offset from the begining of the string where the substring
     * of interest starts.
     */
    public int off;

    /**
     * the length of the substring of interest.
     */
    public int len;

    /**
     * return the entire string held in this class
     */
    public final String str() { return string; }

    Substring(int offset, int length) {
        off = offset;
        len = length;
    }

    public Substring(char c[], int offset, int length) {
        this(offset, length);
        string = new String(c);
    }

    public Substring(String s, int offset, int length) {
        this(offset, length);
        string = s;
    }

    public Substring(String s) {
        this(s, 0, s.length());
    }

    public String toString() {
        int n = off + len;
        if(len <= 0 || n >= string.length())
            n = string.length();
        return string.substring(off, n);
    }
}
