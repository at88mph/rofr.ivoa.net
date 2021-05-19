package net.ivoa.registry.harvest.iterator;

import java.util.regex.Pattern;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.FileReader;
import java.io.File;
import java.io.FilenameFilter;

import javax.xml.parsers.DocumentBuilder; 

/**
 * Access to set of VOResource documents stored as separate files in a 
 * directory.
 */
public class VOResourceCache implements RecordServer {

    private File indir = null;
    private DocumentBuilder db = null;

    /**
     * Open a directory containing VOResource records
     * @param importDir     the directory to search for records in.
     */
    public VOResourceCache(File importDir) throws FileNotFoundException {
        indir = importDir;

        if (! indir.exists()) 
            throw new FileNotFoundException("Import directory not found: " + 
                                            indir);
    }

    /**
     * return the import directory
     */
    public File getImportDirectory() { return indir; }

    /**
     * return the number of files currently accessible in the cache
     */
    public int size() { return getFiles().length; }

    /**
     * set the document build that should be used when parsing the 
     * records in the cache.  This is used to provide a validating parser.
     * If null, a new, non-validating parser will be used.
     */
    public void setDocumentBuilder(DocumentBuilder builder) { db = builder; }

    /**
     * return an iterator that will serve up the VOResource files found in
     * the import directory
     */
    public DocumentIterator records() {
        return new FileIterator(db);
    }

    private Pattern xmlfilepat = Pattern.compile("[^\\._]?.*\\.xml");
    File[] getFiles() {
        return indir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return xmlfilepat.matcher(name).matches();
                }
            });
    }

    /**
     * a DocumentIterator implementation that lets you know the name of the 
     * source document's filename for each document it returns.  This is the 
     * implementation returned by {@link #records()}.
     */
    public class FileIterator extends DocumentIteratorBase {
        File[] files = null;
        int i = -1;

        FileIterator(DocumentBuilder builder) {
            super(builder);
            files = getFiles();
        }

        public Reader nextReader() throws IOException {
            Reader out = null;
            if (++i < files.length) {
                out = new FileReader(files[i]);
            }
            return out;
        }

        /**
         * return the name of the file behind the last reader returned 
         * by the last call to {@link #nextReader()}
         */
        public String getLastFilename() {
            if (i < 0) 
                return null;
            else if (i >= files.length) 
                return files[files.length-1].getName();
            else 
                return files[i].getName();
        }
    }
}
