package net.ivoa.registry.harvest;

import java.io.Writer;
import java.io.IOException;

/**
 * An interface for receiving the VOResource records harvested from a 
 * registry.  This interface determines the manner in which harvested 
 * records are persisted and synchronously (with the harvesting thread) 
 * processed.  
 * <p>
 * When harvesting starts, the harvesting process initializes the consumer by
 * calling  {@link #harvestStarting()}.  The harvester then sends each record
 * to the consumer by first calling  {@link #getRecordWriter()} and then 
 * writing to the returned Writer.  When harvesting is finished, the 
 * consumer's  {@link #harvestDone(boolean) harvestDone()}, giving the consumer 
 * the chance to apply some action to the complete set of harvested records.  
 */
public interface HarvestConsumer {

    /**
     * indicate that the harvesting process is starting.  This consumer 
     * should intitialize itself and prepare for calls to 
     * {@link #getRecordWriter()} 
     * @throws IOException    if the consumer fails to initialize
     * @throws IllegalStateException   if called out of logical order: if 
     *     after a previous harvestStarting() but before a corresponding 
     *     call to harvestDone(), or if called after harvestDone() but when
     *     restarting is not possible (i.e. canRestart() returns false).  
     */
    public void harvestStarting() throws IOException;

    /**
     * provide a resource record to the consumer.
     * @throws IllegalStateException   if {@link #harvestStarting()} was 
     *            not called prior to a call to this function, or if this 
     *            function was called after a call to 
     *            {@link #harvestDone(boolean) harvestDone()}.  
     * @throws IOException    if a failure occurs while reading the record
     *            contents.
     */
    public void consume(HarvestedRecord record) throws IOException;

    /**
     * indicate that the harvesting process is finished.  No more calls
     * to {@link #harvestDone(boolean) harvestDone()} should be expected 
     * after the call to this function.  
     * <p>
     * Through a call to this function, implementations can trigger futher 
     * handling on the received records as a whole.  The input boolean allows
     * the caller to indicate whether harvesting was run to a successful 
     * completion; it is up to the consumer to decide how to proceed.  
     * @param successfully  if true, the harvesting was completed successfully;
     *                      otherwise, the harvesting is being interrupted 
     *                      prematurely either by request or due to an 
     *                      unrecoverable error.
     */
    public void harvestDone(boolean successfully);

    /**
     * return true if it is possible to restart harvesting after a call to 
     * {@link #harvestDone(boolean) harvestDone()}.  If true, additional 
     * calls to {@link #consume(HarvestedRecord) consume()} may be resumed 
     * after another call to {@link #harvestStarting()}.  
     * <p>
     * No assumptions are made as to how harvesting is resumed: it could 
     * start over or pick up where the last harvesting process left.  Previously
     * consumed records may or may not be reharvested.  
     * <p>
     * This function can be called at anytime; however, the value returned is 
     * not guaranteed to be the same everytime.  For example, after consuming 
     * some number of records, the consumer may "turn off" its ability to 
     * accept new records.
     */
    public boolean canRestart();
}
