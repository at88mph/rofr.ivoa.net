package net.ivoa.registry.harvest;

import java.io.Writer;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.FileNotFoundException;

/**
 * A HarvestConsumer that writes the harvested records to disk as XML.  
 * <p>
 * This implementation will write each record to a separate file in a 
 * given destination directory.  Each file will have a name built from a 
 * basename and an integer indicating the order the record was harvested in.  
 * <p>
 * This implementation does not actually keep track of whether 
 * {@link harvestStarting()} or {@link harvestDone()} have been called; 
 * {@link getRecordWriter()} may be called at anytime.  This class just 
 * makes sure that each record has a unique output name and that previous 
 * records are not overwritten.  
 */
public class CacheToDirectory implements HarvestConsumer {

    /**
     * the directory where records will be cached to. 
     */
    protected File dest = null;

    static final String deletedList = "_deleted.lis";
    static final String missingList = "_missing.lis";

    /*
     * a basename from which output filename (in dest) will be constructed.
     */
    String basename = null;

    /*
     * an output file counter that is used to construct unique output filenames
     */
    int nextNum = 1;  

    /**
     * create the caching consumer
     * @param directory    the directory to cache the VOResource files into; 
     *                       this directory must exist.  
     * @param basename     a basename to form the output filenames.  Each
     *                       VOResource files will be called 
     *                   <i>basename</i><code>_</code><i>#</i><code>.xml</code>,
     *                       where <i>#</i> is an integer.  
     */
    public CacheToDirectory(File directory, String basename) 
        throws IOException
    {
        if (! directory.exists()) 
            throw new FileNotFoundException("Directory not found: " + directory);
        if (! directory.isDirectory())
            throw new IOException("Not a directory: " + directory);
        if (! directory.canWrite())
            throw new IOException("Write permission denied: " + directory);
        if (basename == null) 
            throw new NullPointerException("Record file basename not provided");

        dest = directory;
        this.basename = basename;
    }

    /**
     * create the caching consumer
     * @param directory    the directory to cache the VOResource files into; 
     *                       this directory must exist.  
     * @param basename     a basename to form the output filenames.  Each
     *                       VOResource files will be called 
     *                   <i>basename</i><code>_</code><i>#</i><code>.xml</code>,
     *                       where <i>#</i> is an integer.  
     * @param initCounter  the integer value to give to the first harvested 
     *                       record when constructing its filename.  
     */
    public CacheToDirectory(File directory, String basename, int initCounter) 
        throws IOException
    {
        this(directory, basename);
        nextNum = initCounter;
    }

    /**
     * indicate that the harvesting process is starting.  This implementation
     * does nothing.
     */
    @Override
    public void harvestStarting() { }

    /**
     * consume a harvested resource record by writing it to a unique file 
     * on disk.  
     * @throws IllegalStateException   if {@link #harvestStarting()} was 
     *            not called prior to a call to this function, or if this 
     *            function was called after a call to 
     *            {@link #harvestDone(boolean) harvestDone()}.  
     * @throws IOException    if a failure occurs while reading the record
     *            contents.
     */
    @Override
    public void consume(HarvestedRecord record) throws IOException {
        if (record.isAvailable()) {
            File outfile = nextFile();

            while (outfile.exists()) { 
                takeNextFilename();
                outfile = nextFile();
            }
            takeNextFilename();

            Writer out = new FileWriter(outfile);
            record.writeContent(out);
            out.close();
        }
        else {
            File outfile = 
               new File(dest, (record.isDeleted()) ? deletedList : missingList);
            PrintWriter out = 
                new PrintWriter(new FileWriter(outfile, true));
            out.println(record.getID());
            out.close();
        }
    }


    /**
     * return the name of the next output file to be written to
     */
    public String nextFilename() {
        StringBuilder outfile = new StringBuilder(basename);
        outfile.append('_').append(nextNum).append(".xml");
        return outfile.toString();
    }

    /**
     * return the next output file to be written to.   This calls 
     * {@link #nextFilename()} and combines it with the destination 
     * directory for a complete file path. 
     */
    protected File nextFile() {
        return new File(dest, nextFilename());
    }
    

    /**
     * claim the use of the filename returned by {@link #nextFilename()}.  This 
     * will cause nextFilename() to return a new filename.  
     */
    protected void takeNextFilename() {
        nextNum++;
    }

    /**
     * indicate that the harvesting process is finished.  This implementation 
     * does nothing.  
     * @param successfully  if true, the harvesting was completed successfully;
     *                      otherwise, the harvesting is being interrupted 
     *                      prematurely either by request or due to an 
     *                      unrecoverable error.
     */
    @Override
    public void harvestDone(boolean successfully) { }

    /**
     * return true if it is possible to restart harvesting after a call to 
     * {@link #harvestDone(boolean) harvestDone()}.  If true, additional 
     * calls to {@link #getRecordWriter()} may be resumed after another 
     * call to {@link #harvestStarting()}.  
     * <p>
     * This implementation will always return true unless something 
     * happens to the output directory. 
     */
    @Override
    public boolean canRestart() {
        return (dest.exists() && dest.canWrite());
    }


}