package edu.harvard.cda.rofr_identities_harvesting.main;

import edu.harvard.cda.rofr_identities_harvesting.util.ConsoleANSIColorCodes;

public final class HighLevelUsagePrinter {

    private HighLevelUsagePrinter() {}

    public static void print() {
        System.out.printf("\n\n%sNB:%s This short guide is meant to provide high-level usage orientation. Use the %s[-h]%s option"
                          +"\n    to find out more about individual options."
                          , ConsoleANSIColorCodes.RED
                          , ConsoleANSIColorCodes.RESET
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET);
        System.out.printf("\n");
        System.out.printf("\nThis application is used to download the Registry resources that can be obtained by"
                          +"\ntriggering the OAI-PMH response to the 'Identify' verb on a registry's OAI-PMH endpoint."
                          +"\n"
                          +"\nThe application can be used in two modes:"
                          +"\n"
                          +"\n    - download the Registry resources of all registries listed in some RofR server"
                          +"\n    - download the Registry resources of one or more targeted registries"
                          +"\n"
                          +"\nIt is also possible to combine both modes by using the %s'-r'%s and"
                          +"\nthe %s'-t'%s options together (see below)."
                          +"\n"
                          +"\nRegardless of the mode, the general contract for supplying command line arguments is:"
                          +"\n"
                          +"\n    %sjava -jar path/to/uber.jar [global options] [COMMAND] [command-specific options]%s"
                          +"\n"
                          +"\nOnly a single COMMAND is supported: 'harvest'"
                          +"\n"
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET);
                          

        System.out.printf( "\n %sDownload the Registry resources of all registries listed in some RofR server%s"
                          +"\n %s============================================================================%s"
                           , ConsoleANSIColorCodes.BRIGHT_YELLOW
                           , ConsoleANSIColorCodes.RESET
                           , ConsoleANSIColorCodes.YELLOW
                           , ConsoleANSIColorCodes.RESET);
        System.out.printf("\n");
        System.out.printf("\nIn this mode, the goal is to download the Resource records for all registries listed"
                          +"\nin some RofR server. The application only needs to be supplied the hostname of the RofR"
                          +"\n(not the entire URL to the OAI-PMH endpoint). This is because this application is built"
                          +"\nwith an understanding of the particular software that is currently used to host the RofR."
                          +"\nThe %s'-r'%s option is used to supply the RofR hostname"
                          +"\n"
                          +"\nA sample, quite minimal (in terms of command line arguments) incantation might be"
                          +"\nthe following:"
                          +"\n"
                          +"\n%s    java -jar path/to/uber.jar harvest -r http://rofr.ivoa.net -d /some/dir -p false%s"
                          +"\n"
                          +"\nThe above incantation will dump the resource records of all registries at the RofR in"
                          +"\n the %s/some/dir%s directory without pretty-printing them (%s-p false%s)"
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET                          
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET);                          
        System.out.printf("\n\n\nA more substantial incantation is the following:"
                         +"\n"
                         +"\n    %sjava -jar path/to/uber.jar -l off harvest -r http://rofr.ivoa.net -v -d /some/dir -p true -s%s"
                         +"\n"
                         +"\nThe above incantation turns off all logging, creates alongside each resource a 'provenance' file"
                         +"\nand prints a summary at the end. Use the %s-h%s option to find more about the command line arguments."
                         +"\n\n"
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET
                          , ConsoleANSIColorCodes.GREEN);

        System.out.printf( "\n %sDownload the Registry resources of specific registries%s"
                          +"\n %s======================================================%s"
                           , ConsoleANSIColorCodes.BRIGHT_YELLOW
                           , ConsoleANSIColorCodes.RESET
                           , ConsoleANSIColorCodes.YELLOW
                           , ConsoleANSIColorCodes.RESET);        
        System.out.printf("\n\nIn this mode we are using the %s'-t'%s option to directly supply the OAI-PMH endpoints"
                          +"\nof one (or more) registries and the application will simply download the Registry"
                          +"\nresources from those registries. This mode may come in handy if you need to download"
                          +"\nthe Registry resource of a registry that's not hosted in the RofR."
                          +"\nA minimal invocation is the following:"
                          +"\n"
                          +"\n%sjava -jar path/to/uber.jar harvest -p true -d /some/dir -t http://registry.euro-vo.org/oai.jsp%s"
                          +"\n"
                          +"\nThe above incantation will download the Registry resource of the EURO-VO registry. Observe that"
                          +"\nyou need to supply the base OAI-PMH endpoint. If you wish to download Registry resources"
                          +"\nfrom more than one registry simply use %s-t URL_1 URL_2 URL_3 ...%s; i.e. you don't have to repeat"
                          +"\nthe %s'-t'%s option."
                          +"\n\n\n"
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET                          
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET
                          , ConsoleANSIColorCodes.GREEN
                          , ConsoleANSIColorCodes.RESET);                          
                          
    }


}
