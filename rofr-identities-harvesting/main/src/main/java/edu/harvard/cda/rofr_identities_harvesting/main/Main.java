package edu.harvard.cda.rofr_identities_harvesting.main;

import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.net.URL;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;

import com.beust.jcommander.ParameterException;

import edu.harvard.cda.jutil.cli.jcommander.CommandCLIHelper;
import edu.harvard.cda.jutil.cli.jcommander.ParameterValidationException;
import edu.harvard.cda.jutil.cli.jcommander.CommandInfo;

import edu.harvard.cda.rofr_identities_harvesting.util.XMLDeclarationOption;
import edu.harvard.cda.rofr_identities_harvesting.util.XMLPrettyPrintingOption;

import edu.harvard.cda.jutil.rest.HttpStatusNotOKish;

public final class Main {

    private Main() {}

    final static Logger logger = Logger.getLogger(Main.class);

    public static void main(String args[]) throws IOException
                                                  , ParserConfigurationException
                                                  , SAXException
                                                  , XPathExpressionException
                                                  , TransformerException
                                                  , IOException
                                                  , HttpStatusNotOKish {
        final String arguments = Joiner.on(" ").join(args);
        CommandCLIHelper<MainCLI> cliHelper = null;
        try {
            cliHelper = new CommandCLIHelper<>(MainCLI.class.getName()
                                               , MainCLI.class
                                               , MainCLI.COMMAND.HARVEST.getName(), HarvestCLI.class
                                               );

            CommandInfo<MainCLI> cli = cliHelper.parse(args);
            if (cli.mainParams.help) {
                if (cli.commandName==null) {
                    System.out.printf("%s\n", cliHelper.usage());
                    System.exit(0);
                } else {
                    System.out.printf("when the -h option is passed no command can be given (yet, here, [%s] was given).\n"
                                      , cli.commandName);
                    System.exit(1);
                }
            } else if (cli.mainParams.printUsage) {
                if (cli.commandName==null) {
                    HighLevelUsagePrinter.print();
                    System.exit(0);
                } else {
                    System.out.printf("when the -pu option is passed no command can be given (yet, here, [%s] was given).\n"
                                      , cli.commandName);
                    System.exit(1);
                }
            } else {
                LoggerInitialization.init(DebugLevel.fromCode(cli.mainParams.debugLevel));
                if (cli.commandName!=null) {
                    MainCLI.COMMAND command = MainCLI.COMMAND.fromName(cli.commandName);
                    Assert.assertNotNull(String.format("you should expect to never see this message, if the command "+
                                                       "[%s] was not recognized, parsing of the command line "+
                                                       "arguments should have failed before this point"
                                                       , cli.commandName), command);
                    switch (command) {
                    case HARVEST: {
                        final HarvestCLI commandCLI = (HarvestCLI) cli.commandParams;
                        final Path dir = Paths.get(commandCLI.directory);
                        final XMLPrettyPrintingOption xmlPrettyPrintingOption = commandCLI.prettyPrint?
                             XMLPrettyPrintingOption.SPACE_4
                            :XMLPrettyPrintingOption.OFF;
                        Files.createDirectories(dir);
                        CommandHarvest.exec(arguments
                                            , commandCLI.rofrServer==null?null:new URL(commandCLI.rofrServer)
                                            , (commandCLI.targetedRegistries==null)?new ArrayList<String>():commandCLI.targetedRegistries
                                            , dir
                                            , commandCLI.provenance
                                            , commandCLI.includeXMLDeclaration?XMLDeclarationOption.INCLUDE:XMLDeclarationOption.OMIT
                                            , xmlPrettyPrintingOption
                                            , FilenameGenerationStrategy.fromCode(commandCLI.fnameGenStrategy)
                                            , commandCLI.printSummary);
                        break;
                    }
                            
                    default:
                        Assert.fail(String.format("Unhandled case [%s]", command));
                    }
                } else {
                    throw new ParameterValidationException("A command has to be provided when the '-h' flag is not set.");
                }
            }
        } catch (ParameterException | ParameterValidationException e) {
            e.printStackTrace();
            System.out.printf("%s\n", cliHelper.usage());
            System.exit(1);
        }
    }
}
