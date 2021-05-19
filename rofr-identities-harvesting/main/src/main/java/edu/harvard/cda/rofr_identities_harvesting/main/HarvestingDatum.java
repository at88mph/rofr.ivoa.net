package edu.harvard.cda.rofr_identities_harvesting.main;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;

import edu.harvard.cda.rofr_identities_harvesting.util.RegistryInfo;

public final class HarvestingDatum {

    public final RegistryInfo registryInfo;
    public final String       accessURL;
    public final Long         ms;
    public final String       filenameWtErrorTrace;

    public HarvestingDatum(  final RegistryInfo registryInfo
                           , final String       accessURL
                           , final Long         ms
                           , final String       filenameWtErrorTrace ) {
        Assert.assertFalse("if no error trace was logged, then it follows that we were"
                           +" able to calculate duration to fetch response, so [ms] can't be null"
                           , (ms == null) && (filenameWtErrorTrace == null) );
        this.registryInfo         = registryInfo;
        this.accessURL            = accessURL;
        this.ms                   = ms;
        this.filenameWtErrorTrace = filenameWtErrorTrace;
    }

    public static int succCount(final Collection<HarvestingDatum> xs) {
        int rv = 0;
        for (final HarvestingDatum x: xs) {
            if (x.filenameWtErrorTrace == null)
                rv++;
        }
        return rv;
    }

    public static int failCount(final Collection<HarvestingDatum> xs) {
        int rv = 0;
        for (final HarvestingDatum x: xs) {
            if (x.filenameWtErrorTrace != null)
                rv++;
        }
        return rv;
    }

    public static List<String> fnamesWithErrorTraces(final Collection<HarvestingDatum> xs) {
        final List<String> rv = new ArrayList<>();
        for (final HarvestingDatum x: xs) {
            if (x.filenameWtErrorTrace != null)
                rv.add(x.filenameWtErrorTrace);
        }
        return rv;
    }
}
