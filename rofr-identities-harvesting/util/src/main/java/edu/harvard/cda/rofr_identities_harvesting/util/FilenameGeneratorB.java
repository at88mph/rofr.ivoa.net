package edu.harvard.cda.rofr_identities_harvesting.util;

import java.util.regex.Pattern;

public class FilenameGeneratorB implements IFilenameGenerator {

    public String generateFromRegistryInfo(final RegistryInfo ri
                                           , final String fileExtension) {
        final String core = ri.identifier
            .replaceFirst(Pattern.quote("ivo://"), "")
            .replace("/", "_");
        return fileExtension==null?core
            :String.format("%s.%s", core, fileExtension);
    }

}
