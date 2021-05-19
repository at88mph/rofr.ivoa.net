package edu.harvard.cda.rofr_identities_harvesting.util;

import java.io.IOException;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.charset.StandardCharsets;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
    
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;


import com.google.common.io.CharSink;


public final class Util {

    private Util() {}

    public static boolean isDirectoryEmpty(final String directory) {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(directory))) {
            return !dirStream.iterator().hasNext();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void dumpToFileDeleteIfExists(final Path parentDir, final String fname, final String s) throws IOException {
        final Path f = parentDir.resolve(fname);
        Files.deleteIfExists(f);
        Util.dumpToFile(s, Files.createFile(f));
    }

    public static void dumpToFile(final String s, final Path p) throws IOException {
        final CharSink sink = com.google.common.io.Files.asCharSink(p.toFile(), StandardCharsets.UTF_8);
        sink.write(s);
    }

    public static final XPath xpath() {
        final XPathFactory xPathfactory = XPathFactory.newInstance();
        return xPathfactory.newXPath();
    }

    public static final NodeList getNodes(final XPath xpath, final Object context, final String expression) throws XPathExpressionException {
        final XPathExpression expr = xpath.compile(expression);
        final NodeList rv = (NodeList) expr.evaluate(context, XPathConstants.NODESET);
        return rv;
    }

    public static final Node getNode(final XPath xpath, final Object context, final String expression) throws XPathExpressionException {
        final XPathExpression expr = xpath.compile(expression);
        final Node rv = (Node) expr.evaluate(context, XPathConstants.NODE);
        return rv;
    }    

}

