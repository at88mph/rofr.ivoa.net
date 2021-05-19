package edu.harvard.cda.rofr_identities_harvesting.util;

public class FilenameGeneratorA implements IFilenameGenerator {

    public String generateFromRegistryInfo(final RegistryInfo ri
                                           , final String fileExtension) {
        final String core = ri.identifier.replace("://", "_").replace(".", "_").replace("/", "_");
        return fileExtension==null?core
            :String.format("%s.%s", core, fileExtension);
    }

}
