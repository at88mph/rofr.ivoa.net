package edu.harvard.cda.rofr_identities_harvesting.main;

import com.google.common.base.Joiner;

import com.beust.jcommander.Parameter;

import edu.harvard.cda.jutil.cli.jcommander.ParameterValidation;
import edu.harvard.cda.jutil.cli.jcommander.ParameterValidationException;

import edu.harvard.cda.jutil.base.Util;

public class MainCLI {

    public static enum COMMAND {
        HARVEST("harvest")
        ;

        private String name;
        private COMMAND(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static COMMAND fromName(String name) {
            for (COMMAND command: COMMAND.values()) {
                if (command.getName().equals(name))
                    return command;
            }
            return null;
        }        
    }

    @Parameter(names = {"-h", "--help"}, description="print help and exit", required=false)
    public boolean help = false;

    @Parameter(names = {"-l", "--debug-level"}
    , description="debug level. Can be one of: 'off', 'fatal', 'error', 'warn', 'info', 'debug', 'trace' or 'all'"
    , required=false, arity=1)
    public String debugLevel = "info";

    @Parameter(names = {"-p", "--print-high-level-usage"}, description="print high-level usage documentation (include examples) and exit", required=false)
    public boolean printUsage = false;


    @ParameterValidation
    public void validateParams() {
        if (DebugLevel.fromCode(debugLevel) == null)
            throw new ParameterValidationException(String.format("Unrecognized debug level: '%s'. Valid values are: {%s}"
                                                                 , debugLevel
                                                                 , Joiner.on(", ").join(DebugLevel.codes())));
        if (help && printUsage)
            throw new ParameterValidationException("only one of the [-h] and [-p] flags may be set");
    }
}
