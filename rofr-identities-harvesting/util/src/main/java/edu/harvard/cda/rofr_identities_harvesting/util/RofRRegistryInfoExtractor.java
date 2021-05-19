package edu.harvard.cda.rofr_identities_harvesting.util;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import java.net.URL;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.apache.http.Header;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.junit.Assert;

import com.google.common.base.Joiner;


import edu.harvard.cda.jutil.rest.SimpleRestClient;
import edu.harvard.cda.jutil.xml.DOMUtils;


public final class RofRRegistryInfoExtractor {

    private RofRRegistryInfoExtractor() {}

    private static final Logger logger = Logger.getLogger(RofRRegistryInfoExtractor.class);

    public static final List<RegistryInfo> extractRegistriesInfo(final URL rofrServer) {
        try {
            final String url = String.format("%s/cgi-bin/oai.pl?verb=ListRecords&metadataPrefix=ivo_vor&set=ivoa_publishers"
                                             , rofrServer);
            logger.debug(String.format("Obtaining the list of registries to harvest from using [%s]\n"
                                       , url));



            final SimpleRestClient     restClient     = new SimpleRestClient    (logger, new ArrayList<Header>());

            final String s = restClient.get(url);
            final List<RegistryInfo> rv = new ArrayList<>();
            final Document doc = DOMUtils.parse(s);
            final XPath xpath = Util.xpath();
            /* TODO: If you want to be rigorous and properly constrain in a namespace-aware manner (obviously using the
                     namespace URIs, not the prefixes) check out the following resources:

                     [1] https://stackoverflow.com/a/6392700/274677
                     [2] https://stackoverflow.com/a/6969800/274677

                     ... and constrain the namespace URIs of both the element local names _and_ the attribute
                     names (if namespaced)
            */
            final NodeList nodes = Util.getNodes(xpath
                                                 , doc
                                                 , "/*[local-name()='OAI-PMH']"
                                                 +"/*[local-name()='ListRecords']"
                                                 +"/*[local-name()='record']"
                                                 +"/*[local-name()='metadata']"
                                                 +"/*[local-name()='Resource']");
            NEXT_RESOURCE:
            for (int i = 0 ; i < nodes.getLength() ; i++) {
                Node node  = nodes.item(i);

                final String title       = ((String) xpath.compile("title"     ).evaluate(node, XPathConstants.STRING)).trim();
                final String shortName   = ((String) xpath.compile("shortName" ).evaluate(node, XPathConstants.STRING)).trim();
                final String identifier  = ((String) xpath.compile("identifier").evaluate(node, XPathConstants.STRING)).trim();
                final String accessURL   = OAIPMHAccessURLExtractor.extractAccessURL(xpath, node);
                if (accessURL==null) {
                    logger.warn(String.format("%sno access URL found for registry %s (%s) - skipping it%s"
                                              , ConsoleANSIColorCodes.YELLOW
                                              , identifier
                                              , title
                                              , ConsoleANSIColorCodes.RESET));
                    continue NEXT_RESOURCE;
                }
                final NodeList nodes2 = Util.getNodes(xpath, node, "managedAuthority");
                final List<String> managedAuthorities = ManagedAuthorityExtractor.extractManagedAuthorities(xpath, node);
                rv.add( new RegistryInfo(title, shortName, identifier, accessURL, managedAuthorities) );
            }
            return rv;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
