package edu.harvard.cda.rofr_identities_harvesting.main;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.net.URL;

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
import org.apache.http.Header;

import com.google.common.base.Throwables;

import edu.harvard.cda.jutil.time.DateFormatUtil;
import edu.harvard.cda.jutil.base.SCAUtils;

import edu.harvard.cda.rofr_identities_harvesting.util.Util;
import edu.harvard.cda.rofr_identities_harvesting.util.RofRRegistryInfoExtractor;
import edu.harvard.cda.rofr_identities_harvesting.util.TargetedRegistryInfoExtractor;
import edu.harvard.cda.rofr_identities_harvesting.util.RegistryInfo;
import edu.harvard.cda.rofr_identities_harvesting.util.OAIPMHUtil;
import edu.harvard.cda.rofr_identities_harvesting.util.OAIPMHIdentifyHelper;
import edu.harvard.cda.rofr_identities_harvesting.util.XMLDeclarationOption;
import edu.harvard.cda.rofr_identities_harvesting.util.XMLPrettyPrintingOption;
import edu.harvard.cda.rofr_identities_harvesting.util.ConsoleANSIColorCodes;
import edu.harvard.cda.rofr_identities_harvesting.util.IFilenameGenerator;
import edu.harvard.cda.rofr_identities_harvesting.util.FilenameGeneratorA;
import edu.harvard.cda.rofr_identities_harvesting.util.FilenameGeneratorB;

import edu.harvard.cda.jutil.rest.SimpleRestClient;
import edu.harvard.cda.jutil.rest.HttpStatusNotOKish;

import javax.xml.parsers.ParserConfigurationException;
    


public final class CommandHarvest {
    final static Logger logger = Logger.getLogger(CommandHarvest.class);

    private static IFilenameGenerator fnameGeneratorFromStrategy(final FilenameGenerationStrategy fgs) {
        IFilenameGenerator rv = null;
        switch (fgs) {
        case ROFR:
            return new FilenameGeneratorB();
        case ALT1:
            return new FilenameGeneratorA();
        default:
            Assert.fail(String.format("Unrecognized %s: %s"
                                      , FilenameGenerationStrategy.class.getName()
                                      , fgs));
            return SCAUtils.CANT_REACH_THIS_LINE(IFilenameGenerator.class);
        }
    }

    public static void exec(final String arguments
                            , final URL rofrServer
                            , final List<String> targetedRegistriesOAIPMHEndPoints
                            , final Path directory
                            , final boolean provenance
                            , final XMLDeclarationOption xmlDeclarationOption
                            , final XMLPrettyPrintingOption xMLPrettyPrintingOption
                            , final FilenameGenerationStrategy fgs
                            , final boolean printSummary
                            ) throws ParserConfigurationException
                                     , SAXException
                                     , XPathExpressionException
                                     , TransformerException
                                     , IOException
                                     , HttpStatusNotOKish {
        final List<RegistryInfo> registriesInfo = new ArrayList<>();
        if (rofrServer!=null) {
            final List<RegistryInfo> registriesInfoFromRofR = RofRRegistryInfoExtractor.extractRegistriesInfo(rofrServer);
            registriesInfo.addAll(registriesInfoFromRofR);
        }
        registriesInfo.addAll(TargetedRegistryInfoExtractor.extractRegistriesInfo(targetedRegistriesOAIPMHEndPoints));
        
        final List<HarvestingDatum> stats = new ArrayList<>();
        for (final RegistryInfo registryInfo: registriesInfo) {
            String identifyResponseURL = null;
            Long ms1 = null;
            Long ms2 = null;
            final String fname = fnameGeneratorFromStrategy(fgs).generateFromRegistryInfo(registryInfo, "xml");
            try {
                logger.debug(registryInfo);
                final SimpleRestClient restClient = new SimpleRestClient (logger, new ArrayList<Header>());
                identifyResponseURL = OAIPMHUtil.identifyResponseURLFromAccessURL(registryInfo.accessURL);
                logger.info(String.format("%sObtaining registry information from%s [%s%s%s]"
                                          , ConsoleANSIColorCodes.brightBlack().onBlack()
                                          , ConsoleANSIColorCodes.RESET
                                          , ConsoleANSIColorCodes.black().onBlue()
                                          , identifyResponseURL
                                          , ConsoleANSIColorCodes.RESET
                                          ));
                ms1 = System.currentTimeMillis();
                final String identifyResponse = restClient.get(identifyResponseURL);
                ms2 = System.currentTimeMillis();
                if (provenance) {
                    final String provenanceContent = String.format("Command line arguments upon the invocation of the application were:\n"
                                                                   +"    %s\n"
                                                                   +"\n\n\n"
                                                                   +"On %s, an HTTP GET to the following URL:\n"
                                                                   +"    %s\n"
                                                                   +"... elicited the following response:\n\n"
                                                                   +"--%%<---------------------------------------------------\n"
                                                                   +"%s\n"
                                                                   +"--------------------------------------------------->%%--\n"
                                                                   +"\n\n"
                                                                   +"The resource was extracted out of the above XML document%s.\n\n\n"
                                                                   , arguments
                                                                   , DateFormatUtil.format02(TimeZone.getTimeZone("UTC"), new Date())
                                                                   , identifyResponseURL
                                                                   , identifyResponse
                                                                   , (xMLPrettyPrintingOption==XMLPrettyPrintingOption.OFF)?"":" and was also subsequently pretty-printed"
                                                                   );
                    Util.dumpToFileDeleteIfExists(directory, provenanceFilename(fname), provenanceContent);
                }
                final OAIPMHIdentifyHelper oAIPMHIdentifyHelper = new OAIPMHIdentifyHelper(xmlDeclarationOption, xMLPrettyPrintingOption);
                final String resource = oAIPMHIdentifyHelper.stripResponse(identifyResponse);
                Util.dumpToFileDeleteIfExists(directory, fname, resource);
                stats.add(new HarvestingDatum(registryInfo
                                              , identifyResponseURL
                                              , ms2-ms1
                                              , (String) null));

            } catch (IOException | HttpStatusNotOKish e) {
                logger.error(String.format("%s%s%s exception: %s%s%s when trying to obtain the Identify\n"
                                           +"response for the [%s] registry; URL was: [%s]"
                                           , ConsoleANSIColorCodes.red().onYellow()
                                           , e.getClass().getName()
                                           , ConsoleANSIColorCodes.RESET
                                           , ConsoleANSIColorCodes.red().onYellow()
                                           , e.getMessage()
                                           , ConsoleANSIColorCodes.RESET
                                           , registryInfo.title
                                           , ConsoleANSIColorCodes.blue().onBlack()
                                           , identifyResponseURL
                                           , ConsoleANSIColorCodes.RESET));
                            
                logger.error(String.format("Full trace is %s%s%s"
                                           , ConsoleANSIColorCodes.brightRed().onBlack()
                                           , Throwables.getStackTraceAsString(e)
                                           , ConsoleANSIColorCodes.RESET));
                final String provenanceContent = String.format("%s exception: [%s] when trying to obtain the Identify response\n"
                                                               +"for the [%s] registry; URL was: [%s]. Full trace is given\n"
                                                               +"below:\n%s\n"
                                                               , e.getClass().getName()
                                                               , e.getMessage()
                                                               , registryInfo.title
                                                               , identifyResponseURL
                                                               , Throwables.getStackTraceAsString(e));
                Util.dumpToFileDeleteIfExists(directory, errorFilename(fname), provenanceContent);
                stats.add(new HarvestingDatum(registryInfo
                                              , identifyResponseURL
                                              , ((ms1!=null)&&(ms2!=null))?(ms2-ms1):null
                                              , directory.resolve(errorFilename(fname)).toString()));
                continue;
            }
        }
        Assert.assertEquals(stats.size(), registriesInfo.size());
        if (printSummary) {
            System.out.printf("\n\n\n\n");
            System.out.printf("%s           S U M M A R Y    R E S U L T S            %s\n"+
                              "%s           ==============================            %s\n\n"
                              , ConsoleANSIColorCodes.yellow().onBlack()
                              , ConsoleANSIColorCodes.RESET
                              , ConsoleANSIColorCodes.yellow().onBlack()
                              , ConsoleANSIColorCodes.RESET                              
                              );
            System.out.printf("\n%s%d%s registries queried. %s%d%s registry resources successfully extracted"
                              , ConsoleANSIColorCodes.blue().onBlack()
                              , stats.size()
                              , ConsoleANSIColorCodes.RESET
                              , ConsoleANSIColorCodes.blue().onBlack()
                              , HarvestingDatum.succCount(stats)
                              , ConsoleANSIColorCodes.RESET);
            if (HarvestingDatum.failCount(stats)>0) {
                System.out.printf("; Extraction of %s%d resources failed%s (see below for details)\n"
                                  , ConsoleANSIColorCodes.black().onRed()
                                  , HarvestingDatum.failCount(stats)
                                  , ConsoleANSIColorCodes.RESET);
            } else {
                System.out.printf(".\n");
            }
            System.out.printf("\n\n\n");
            for (final HarvestingDatum harvestingDatum : stats) {
                if (harvestingDatum.filenameWtErrorTrace==null)
                    System.out.printf("%ssuccess%s%s%8d ms%s to obtain response from: %s%s%s\n"
                                      , ConsoleANSIColorCodes.black().onGreen()
                                      , ConsoleANSIColorCodes.RESET
                                      , ConsoleANSIColorCodes.blue().onBlack()
                                      , harvestingDatum.ms
                                      , ConsoleANSIColorCodes.RESET
                                      , ConsoleANSIColorCodes.blue().onBlack()
                                      , harvestingDatum.registryInfo.accessURL
                                      , ConsoleANSIColorCodes.RESET);
            }
            for (final HarvestingDatum harvestingDatum : stats) {
                if (harvestingDatum.filenameWtErrorTrace!=null)
                    System.out.printf("%sfailure%s while obtaining or processing the response"
                                      +" from %s%s%s - please examine file: %s%s%s\n"
                                      , ConsoleANSIColorCodes.brightRed().onBlack()
                                      , ConsoleANSIColorCodes.RESET
                                      , ConsoleANSIColorCodes.blue().onBlack()
                                      , harvestingDatum.registryInfo.accessURL
                                      , ConsoleANSIColorCodes.RESET
                                      , ConsoleANSIColorCodes.brightRed().onBlack()
                                      , harvestingDatum.filenameWtErrorTrace
                                      , ConsoleANSIColorCodes.RESET);
            }
            System.out.printf("\n\n\n");
        }
    }

    private static final String provenanceFilename(final String fname) {
        return String.format("%s.PROVENANCE", fname);
    }

    private static final String errorFilename(final String fname) {
        return String.format("%s.ERROR", fname);
    }    

}

