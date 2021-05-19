package edu.harvard.cda.rofr_identities_harvesting.util;

import javax.ws.rs.core.UriBuilder;


public final class OAIPMHUtil {

    private OAIPMHUtil() {}


    public static String identifyResponseURLFromAccessURL(final String accessURL) {
        return UriBuilder.fromUri(accessURL).queryParam("verb", "Identify").build().toString();
    }


}
