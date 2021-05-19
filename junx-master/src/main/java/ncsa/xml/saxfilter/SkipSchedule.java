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
 *  2001/09/17  rlp  initial version
 *  2007/11/16  rlp  reconstructed from jad-decompiled class file
 */
package ncsa.xml.saxfilter;

import java.util.Vector;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SkipSchedule {
    TreeMap sched;
    boolean skipping;

    public SkipSchedule(boolean startskip) {
        sched = new TreeMap();
        skipping = false;
        skipping = startskip;
    }

    public synchronized void skipFrom(long pos) {
        if(pos <= 0L)
            skipping = true;
        else
            sched.put(new Long(pos), Boolean.TRUE);
    }

    public synchronized void skipTo(long pos) {
        if(pos <= 0L)
            skipping = false;
        else
            sched.put(new Long(pos), Boolean.FALSE);
    }

    public synchronized long nextSwitch() {
        Set keys = sched.keySet();
        for(Iterator iter = keys.iterator(); iter.hasNext();)
        {
            Long pos = (Long)iter.next();
            if(((Boolean)sched.get(pos)).booleanValue() != skipping)
                return pos.longValue();
        }

        return 0x7fffffffffffffffL;
    }

    public final synchronized boolean skippingAt(long pos) {
        SortedMap upto = sched.headMap(new Long(pos));
        if(upto.size() == 0)
            return skipping;
        else
            return ((Boolean)sched.get(upto.lastKey())).booleanValue();
    }

    public final boolean skipping() {
        return skipping;
    }

    public synchronized boolean popTo(long pos) {
        Boolean last = null;
        Set remove = sched.headMap(new Long(pos + 1L)).keySet();
        for(Iterator iter = remove.iterator(); iter.hasNext(); iter.remove())
            last = (Boolean)sched.get(iter.next());

        if(last != null)
            skipping = last.booleanValue();
        return skipping;
    }

    public synchronized void insert(long start, long change) {
        if(sched.size() == 0 || change == 0L)
            return;
        java.util.Map.Entry pair = null;
        Object newpair[] = null;
        Long pos = null;
        Vector newentries = new Vector(sched.size());
        Iterator iter = sched.entrySet().iterator();
        do
        {
            if(!iter.hasNext())
                break;
            pair = (java.util.Map.Entry)iter.next();
            pos = (Long)pair.getKey();
            if(start < pos.longValue())
            {
                newpair = (new Object[] {
                    new Long(pos.longValue() + change), pair.getValue()
                });
                newentries.addElement(((Object) (newpair)));
                iter.remove();
            }
        } while(true);
        for(iter = newentries.iterator(); iter.hasNext(); sched.put(newpair[0], newpair[1]))
            newpair = (Object[])iter.next();

    }
}
