package edu.harvard.cda.rofr_identities_harvesting.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;


import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import org.junit.Assert;

import org.apache.log4j.Logger;

import javax.xml.parsers.ParserConfigurationException;

public final class OAIPMHIdentifyHelper {

    final static Logger logger = Logger.getLogger(OAIPMHIdentifyHelper.class);

    private final boolean omitXMLDeclaration;
    private final boolean prettyPrinting;
    private final Integer prettyPrintingIndent;

    public OAIPMHIdentifyHelper(final XMLDeclarationOption xmlDeclarationOption
                                , final XMLPrettyPrintingOption xMLPrettyPrintingOption) {
        if (xmlDeclarationOption == XMLDeclarationOption.OMIT)
            omitXMLDeclaration = true;
        else
            omitXMLDeclaration = false;
        prettyPrinting       = (xMLPrettyPrintingOption==XMLPrettyPrintingOption.OFF)?false:true;
        prettyPrintingIndent = (xMLPrettyPrintingOption==XMLPrettyPrintingOption.OFF)?null:xMLPrettyPrintingOption.getIndentAmount();
        Assert.assertTrue( (( prettyPrinting==true  ) && ( prettyPrintingIndent!=null )) ||
                           (( prettyPrinting==false ) && ( prettyPrintingIndent==null )) );
    }

    public String stripResponse(final String s) throws ParserConfigurationException
                                                       , SAXException
                                                       , XPathExpressionException
                                                       , TransformerException
                                                       , IOException {

        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        final Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(s)));

        final XPath xPath = XPathFactory.newInstance().newXPath();
        /*  Going namespace agnostic is not just a display of cavalier attitude; some
         *  publishing registries are quite ignorant when it comes to XML namespaces so we're
         *  trying to accomodate those as well.
         */

        final String lastComponentOfXPath="/*[local-name()='Resource']";
        /*  
         *  Initially (before 2019-09-23) it was attempted to keep this class IVOA-agnostic. I.e. to only
         *  assume: (a) a standard OAIPMH implementation and (b) the expected response to
         *  the Identify verb. As such I had tried to simply use a wildcard for the local name of the
         *  last element at the tip of the XPath expression and so I had the following as the value
         *  of [lastComponentOfXPath]:
         *
         *      "/*"
         *  
         *  This failed when we encountered a response from NASA's HEASARC registry which had multiple
         *  <oai:description> elements (and not just a single one in which the Resource resided).
         *  This was unusual but valid under the OAIPMH XSD grammar as we explicitly have:
         *
         *      <element name="description" type="oai:descriptionType" minOccurs="0" maxOccurs="unbounded"/>
         *
         *  To address that I had to use the above [lastComponentOfXPath] value instead so this class
         *  is no longer IVOA-agnostic.
         *
         *  PS. The NASA's HEASARC registry response is now captured as a test case.
         *
         */ 

        final String xPathExprToGetTheResource = "/*[local-name()='OAI-PMH']"
            +"/*[local-name()='Identify']"
            +"/*[local-name()='description']"
            +lastComponentOfXPath;
        



        final Node result = (Node) xPath.evaluate(xPathExprToGetTheResource, doc, XPathConstants.NODE);
        return nodeToString(result);

    }

    private String nodeToString(Node node) throws TransformerException {
        final StringWriter buf = new StringWriter();
        final Transformer xform = TransformerFactory.newInstance().newTransformer();
        xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration?"yes":"no");
        xform.setOutputProperty(OutputKeys.STANDALONE, "yes");

        if (prettyPrinting) {
            xform.setOutputProperty(OutputKeys.INDENT, "yes");
            xform.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(prettyPrintingIndent));
        } else {
            xform.setOutputProperty(OutputKeys.INDENT, "no");
        }

        xform.transform(new DOMSource(node), new StreamResult(buf));
        return(buf.toString());
    }    



}
