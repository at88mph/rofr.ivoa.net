package edu.harvard.cda.rofr_identities_harvesting.util;

public interface IFilenameGenerator {

    String generateFromRegistryInfo(final RegistryInfo ri
                                    , final String fileExtension);


}
