package edu.harvard.cda.rofr_identities_harvesting.util;

import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.matchers.JUnitMatchers.both;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.junit.matchers.JUnitMatchers.everyItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;


import org.junit.rules.ErrorCollector;
import org.junit.Rule;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.CharSink;
import com.google.common.io.Closeables;

class ConstructorSupplier {
    private String fnameOAIPMHResponse;
    private String fnameResource;

    public ConstructorSupplier(final String fnameOAIPMHResponse, final String fnameResource) {
        this.fnameOAIPMHResponse = fnameOAIPMHResponse;
        this.fnameResource = fnameResource;
    }

    public Object[] supplyArguments() {
        try {
            final File directory=new File("test/test-files/");
            if (!directory.isDirectory())
                throw new RuntimeException(String.format("File [%s] is not a directory or does not exist!"
                                                         , directory.getPath()));
            final File oaipmhResponse = new File(directory, fnameOAIPMHResponse);
            final File resource       = new File(directory, fnameResource);

            return new Object[]{fnameOAIPMHResponse
                                , fileToString(oaipmhResponse)
                                , fileToString(resource)};
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String fileToString(final File file) throws IOException {
        if (!file.isFile())
            throw new RuntimeException(String.format("File [%s] is not a file or does not exist!"
                                                     , file.getPath()));
        final InputStream in = new FileInputStream(file);
        final String content = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
        Closeables.closeQuietly(in);
        return content;
    }

}


@RunWith(Parameterized.class)
public class OAIPMHIdentifierHelperTest {

    private final String fname;
    private final String oaipmhResponse;
    private final String expectedResource;



    public OAIPMHIdentifierHelperTest(final String fname,
                                      final String oaipmhResponse,
                                      final String expectedResource) {
        this.fname = fname;
        this.oaipmhResponse = oaipmhResponse;
        this.expectedResource = expectedResource;
    }

    private static List<ConstructorSupplier> _data() {
        final List<ConstructorSupplier> rv = new ArrayList<>();
        rv.add(new ConstructorSupplier("nasa.heasarc_registry-oaipmh.xml", "nasa.heasarc_registry-ivoa-resource.xml"));
        return rv;
    }

    @Parameters(name = "{index}: {0} ({1})}")
    public static Collection<Object[]> data() {
        final List<Object[]> rv = new ArrayList<>();        
        for (ConstructorSupplier x: _data())
            rv.add(x.supplyArguments());
        return rv;
    }
    
    @Test
    public void testResourceExtractionAsExpected() {
        try {
            final OAIPMHIdentifyHelper oAIPMHIdentifyHelper = new OAIPMHIdentifyHelper(XMLDeclarationOption.INCLUDE
                                                                                       , XMLPrettyPrintingOption.SPACE_2);
            final String actualResource = oAIPMHIdentifyHelper.stripResponse(oaipmhResponse);
            final File file = new File("test/test-files/actual");
            final CharSink sink = Files.asCharSink(file, StandardCharsets.UTF_8);
            sink.write(actualResource);
            Assert.assertEquals(expectedResource, actualResource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}



