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


public final class OAIPMHAccessURLExtractor {

    private OAIPMHAccessURLExtractor() {}

    private static final Logger logger = Logger.getLogger(OAIPMHAccessURLExtractor.class);

    public static String extractAccessURL(final XPath xpath, final Node resourceNode) throws XPathExpressionException {
        final NodeList nodes2 = Util.getNodes(xpath, resourceNode
                                              , "*[local-name()='capability' and @*[local-name()='type' and .='vg:Harvest']]"
                                              +"/*[local-name()='interface' and @*[local-name()='type' and .='vg:OAIHTTP'] and @role='std']"
                                              +"/*[local-name()='accessURL']");
        List<String> accessURLs = new ArrayList<>();
        for (int j = 0 ; j < nodes2.getLength() ; j++) {
            Node node2 = nodes2.item(j);
            final String anAccessURL = ((String) xpath.compile(".").evaluate(node2, XPathConstants.STRING)).trim();
            accessURLs.add(anAccessURL);
        }
        if (accessURLs.size()>1) {
            final List<String> accessURLsWithoutDuplicates = new ArrayList<>(new LinkedHashSet<>(accessURLs));
            final String msg = String.format("%s%d access URLs were found, of which %d are distinct: %s, using the first one (%s)%s"
                                             , ConsoleANSIColorCodes.YELLOW
                                             , accessURLs.size()
                                             , accessURLsWithoutDuplicates.size()
                                             , Joiner.on(", ").join(accessURLsWithoutDuplicates)
                                             , accessURLsWithoutDuplicates.get(0)
                                             , ConsoleANSIColorCodes.RESET);
            if (accessURLsWithoutDuplicates.size()>1)
                logger.warn(msg);
            else
                logger.debug(msg);
            return accessURLsWithoutDuplicates.get(0);
        } else if (accessURLs.size()==0) {
            return null;
        } else {
            Assert.assertEquals(accessURLs.size(), 1);
             return accessURLs.get(0);
        }

    }




}
