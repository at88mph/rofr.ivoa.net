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


public final class ManagedAuthorityExtractor {

    private ManagedAuthorityExtractor() {}

    private static final Logger logger = Logger.getLogger(ManagedAuthorityExtractor.class);

    public static List<String> extractManagedAuthorities(final XPath xpath, final Node resourceNode) throws XPathExpressionException {
        final NodeList nodes2 = Util.getNodes(xpath, resourceNode, "managedAuthority");
        final List<String> managedAuthorities = new ArrayList<>();
        for (int j = 0 ; j < nodes2.getLength() ; j++) {
            Node node2 = nodes2.item(j);
            final String managedAuthority = ((String) xpath.compile(".").evaluate(node2, XPathConstants.STRING)).trim();
            managedAuthorities.add(managedAuthority);
        }
        return managedAuthorities;
    }
}
