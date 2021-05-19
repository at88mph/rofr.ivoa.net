package edu.harvard.cda.rofr_identities_harvesting.util;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import java.net.URL;

import java.io.IOException;

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


import edu.harvard.cda.jutil.rest.HttpStatusNotOKish;
import edu.harvard.cda.jutil.rest.SimpleRestClient;
import edu.harvard.cda.jutil.xml.DOMUtils;


public final class TargetedRegistryInfoExtractor {

    private TargetedRegistryInfoExtractor() {}

    private static final Logger logger = Logger.getLogger(TargetedRegistryInfoExtractor.class);

    public static final List<RegistryInfo> extractRegistriesInfo(final List<String> targetedRegistriesOAIPMHEndPoints)
        throws IOException
               , HttpStatusNotOKish
               , XPathExpressionException {
        final List<RegistryInfo> rv = new ArrayList<>();
        for (final String targetedRegistriesOAIPMHEndPoint: targetedRegistriesOAIPMHEndPoints) {
            rv.add(extractRegistryInfo( targetedRegistriesOAIPMHEndPoint ));
        }
        return rv;
    }

    public static RegistryInfo extractRegistryInfo(final String targetedRegistriesOAIPMHEndPoint)
        throws IOException
               , HttpStatusNotOKish
               , XPathExpressionException {
        final String identifyResponseURL = OAIPMHUtil.identifyResponseURLFromAccessURL(targetedRegistriesOAIPMHEndPoint);
        logger.debug(String.format("About to retrieve [%s]", identifyResponseURL));
        final SimpleRestClient restClient = new SimpleRestClient (logger, new ArrayList<Header>());
        final String s = restClient.get(identifyResponseURL);
        logger.trace(String.format("Identify response at [%s] is given below:\n%s"
                                   , identifyResponseURL
                                   , s));
        final Document doc = DOMUtils.parse(s);
        final XPath xpath = Util.xpath();
        /* TODO: If you want to be rigorous and properly constrain in a namespace-aware manner (obviously using the
           namespace URIs, not the prefixes) check out the following resources:

           [1] https://stackoverflow.com/a/6392700/274677
           [2] https://stackoverflow.com/a/6969800/274677

           ... and constrain the namespace URIs of both the element local names _and_ the attribute
           names (if namespaced)
        */
        final Node node = Util.getNode(xpath
                                       , doc
                                       , "/*[local-name()='OAI-PMH']"
                                       + "/*[local-name()='Identify']"
                                       + "/*[local-name()='description']"
                                       + "/*[local-name()='Resource']");
        final String title       = ((String) xpath.compile("title")     .evaluate(node, XPathConstants.STRING)).trim();
        final String shortName   = ((String) xpath.compile("shortName") .evaluate(node, XPathConstants.STRING)).trim();
        final String identifier  = ((String) xpath.compile("identifier").evaluate(node, XPathConstants.STRING)).trim();
        final String accessURL = OAIPMHAccessURLExtractor.extractAccessURL(xpath, node);
        final List<String> managedAuthorities = ManagedAuthorityExtractor.extractManagedAuthorities(xpath, node);
        return new RegistryInfo(title, shortName, identifier, accessURL, managedAuthorities);
    }
}
