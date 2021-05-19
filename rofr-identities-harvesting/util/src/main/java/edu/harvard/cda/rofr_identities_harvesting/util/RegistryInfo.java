package edu.harvard.cda.rofr_identities_harvesting.util;

import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;

import org.junit.Assert;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;


public final class RegistryInfo {


    public final String title;
    public final String shortName;
    public final String identifier;
    public final String accessURL;
    public final List<String> managedAuthorities;

    public RegistryInfo(final String title,
                        final String shortName,
                        final String identifier,
                        final String accessURL,
                        final List<String> managedAuthorities) {
        this.title      = title;
        this.shortName  = shortName;
        this.identifier = identifier;
        this.accessURL  = accessURL;
        this.managedAuthorities = managedAuthorities;
    }

    public RegistryInfo newManagedAuthorities(final List<String> managedAuthorities) {
        return new RegistryInfo(title, shortName, identifier, accessURL, managedAuthorities);
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
            .add("title", title)
            .add("shortName", shortName)
            .add("identifier", identifier)
            .add("accessURL", accessURL)
            .add("managedAuthorities", managedAuthorities)
            ;
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }

    public static TreeMap<String, List<RegistryInfo>> masToRegistryInfo(final List<RegistryInfo> rs) {

        final TreeMap<String, List<RegistryInfo>> rv = new TreeMap<>();
        for (final RegistryInfo r: rs) {
            for (final String ma: r.managedAuthorities) {
                if (!rv.containsKey(ma))
                    rv.put(ma, new ArrayList<RegistryInfo>());
                List<RegistryInfo> ma2ras = rv.get(ma);
                Assert.assertNotNull(ma2ras);
                ma2ras.add(r);
            }
        }
        return rv;
    }

    public static List<String> identifiers(final List<RegistryInfo> ris) {
        final List<String> rv = new ArrayList<String>();
        for (final RegistryInfo ri: ris) {
            rv.add(ri.identifier);
        }
        return rv;
    }
}



