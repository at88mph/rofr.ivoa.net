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
 *  2001/09/18  rlp  initial version
 *  2001/09/24  rlp  added setPause()
 *  2007/11/16  rlp  reconstructed from jad-decompiled class file
 */
package ncsa.xml.saxfilter;

import java.io.IOException;
import java.io.Reader;

/**
 * an interface for controlling the flow from a SAXFilteredReader.  It allows 
 * an XML ContentHandler to skip over sections of the XML document based on
 * its content.  It also allows the data to be altered, either by changing 
 * XML data or inserting new data into the stream sent through the Reader.  
 */
public interface SAXFilterFlowControl {

    /**
     * return a reference to a CharContentLocator for obtaining character
     * positions of content events.  This object is shared with the parser
     * which updates it as XML data is parsed.  Thus, the same locator 
     * should be usable throughout the parsing of a document, making it 
     * necessary to call this method only once. 
     */
    public CharContentLocator getCharLocator();

    /**
     * stop sending characters from the source stream starting at the 
     * given character position.  
     * @param pos   the zero-based character index relative to the start 
     *                  of the file.
     */
    public void skipFrom(long pos);

    /**
     * resume sending characters from the source stream starting at the 
     * given character position.  
     * @param pos   the zero-based character index relative to the start 
     *                  of the file.
     */
    public void resumeFrom(long pos);

    /**
     * pause the sending of characters so that more input data can be parsed
     * until further notice.  This does not cause any data to be skipped;
     * however, by parsing ahead one can issue a skip request starting at 
     * a previously parsed position and guarantee that it is honored.
     */
    public void setPause(boolean pause);

    /**
     * insert a String at the the given position in the stream.
     * @param chars   the character data to insert into the reader's stream.
     * @param pos     the zero-based character index relative to the start
     *                   of the file to insert the string at.  
     * @throws IllegalStateException   if the indicated position has already
     *            been received by the reader.  (That is, you can't go too 
     *            far back in time).
     */
    public void insert(String chars, long pos) throws IOException;

    /**
     * substitute in a string for a range of characters in the stream
     * @param chars  the character data to substitute into the reader's stream
     * @param pos    the zero-based character index relative to the start
     *                   of the file to begin the substitution at. 
     * @param len    the number of characters to pull out of the stream 
     *                   beginning at the pos position.
     */
    public void substitute(String chars, long pos, int len) throws IOException;

    /**
     * push a new input stream onto the stack of input streams.  This is 
     * used for inserting one document into another.  The new document is
     * inserted at the current parsed position (the beginning of the current 
     * SAX event as indicated by the CharContentLocator).
     */
    public void pushSource(Reader src);

    /**
     * schedule the insertion of a marker at the position where we should 
     * pause in the parsing
     */
    public void addPauseMarker(long pos);

    /**
     * set the character that will be used as a pause marker.  This should 
     * be a character that is not normally expected to be encounter in the
     * stream (e.g. \004).
     */
    public char setPauseChar(char c);

    /**
     * return the character that will be used as a pause marker.
     */
    public char getPauseChar();
}
