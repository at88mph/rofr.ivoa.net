package edu.harvard.cda.rofr_identities_harvesting.main;

import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import com.google.common.base.Joiner;
    
import com.beust.jcommander.Parameter;

import edu.harvard.cda.jutil.cli.jcommander.ParameterValidation;
import edu.harvard.cda.jutil.cli.jcommander.ParameterValidationException;

import edu.harvard.cda.rofr_identities_harvesting.util.Util;

public class HarvestCLI {

    @Parameter(names = {"-r", "--rofr-server"}, description="RofR server (e.g. http://rofrbeta.cfa.harvard.edu"
                                                           +" or http://rofr.ivoa.net) to obtain the registries"
                                                           +" to harvest from. NB: either [-r] or [-t], or both"
                                                           +" must be present."
               , required=false, arity=1)
               public String rofrServer;

    @Parameter(names = {"-t", "--targeted-registries"}
               , description="one or more additional registry OAI-PMH endpoint URLs to harvest Identify responses from"
+" - NB: you are supposed to supply the OAI-PMH endoint, not the full URL"
+" that elicits the identify response, e.g. use 'http://registry.euro-vo.org/oai.jsp' instead of"
+" 'http://registry.euro-vo.org/oai.jsp?verb=Identify'. To supply more than one simply append one after the other"
+" (space-separated) without using"
+" the -t parameter again, i.e. like this: -t url1 url2 url3. NB2: either [-t] or [-r], or both, must be present"
               , required=false, variableArity = true)
               public List<String> targetedRegistries;
    

    @Parameter(names = {"-d", "--dir"}, description="directory to write results to", required=true, arity=1)
    public String directory;

    @Parameter(names = {"-v", "--dump-oaipmh-and-provenance"}
               , description="alongside the resources also dump OAI-PMH envelopes and their provenance information"
               , required=false, arity=0)
    public boolean provenance = false;

    @Parameter(names = {"-x", "--XML-declaration"}, description="include XML declaration preamble in the resources"
               , required=false
               , arity=0)
    public boolean includeXMLDeclaration = false;

    @Parameter(names = {"-p", "--pretty-printing"}, description="pretty print the resources. Must either be 'true' or 'false'"
               , required=true, arity=1)
    public boolean prettyPrint = true;

    @Parameter(names = {"-f", "--filename-generation-strategy"}, description="The filename generation strategy to use (based on the IVOID)"
               +". Valid values are 'rofr' or 'alt1'. Use 'rofr' if you want consistency with the filename pattern of the RofR application" 
               , required=true, arity=1)
    public String fnameGenStrategy;
    
    @Parameter(names = {"-s", "--print-summary"}
               , description="print summary statistics at the end. Statistics are always printed regardless of the debug logging level"
               , required=false, arity=0)
    public boolean printSummary = false;


    @ParameterValidation
    public void validateParams() {
        if ((rofrServer==null) && (targetedRegistries==null)) {
            throw new ParameterValidationException("At least one of the [-r] or [-t] parameters must be present");
        }
        
        if (rofrServer != null) {
            try {
                (new URL(rofrServer)).toURI();
            } catch (MalformedURLException | URISyntaxException e) {
                throw new ParameterValidationException(String.format("[%s] (supplied as the -r argument) does not look like a valid URL"
                                                                     , rofrServer));
            }
        }

        if (targetedRegistries != null) {
            for (final String targetedRegistry: targetedRegistries) {
                try {
                    (new URL(targetedRegistry)).toURI();
                } catch (MalformedURLException | URISyntaxException e) {
                    throw new ParameterValidationException(String.format("[%s] (supplied as one of the -t arguments) does not look"
                                                                         +" like a valid URL"
                                                                         , targetedRegistry));
                }
            }
        }
                
        final Path directoryP = Paths.get(directory);
        if (Files.exists(directoryP)) {
            if (!Files.isDirectory(directoryP))
                throw new ParameterValidationException(String.format("File [%s] already exists and is not a directory"
                                                                     , directoryP));
            else if (!Util.isDirectoryEmpty(directory))
                throw new ParameterValidationException(String.format("Directory [%s] is not empty"
                                                                     , directoryP));
        }

        if (fnameGenStrategy != null) {
            final FilenameGenerationStrategy fgs = FilenameGenerationStrategy.fromCode(fnameGenStrategy);
            if (fgs==null)
                throw new ParameterValidationException(String.format("Unrecognized -f parameter: [%s]. Valid values are: %s"
                                                                     , fnameGenStrategy
                                                                     , Joiner.on(", ").join(FilenameGenerationStrategy.codes())));
        }
    }
}
