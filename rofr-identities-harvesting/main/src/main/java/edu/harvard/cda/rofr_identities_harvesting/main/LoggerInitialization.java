package edu.harvard.cda.rofr_identities_harvesting.main;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Level;


public final class LoggerInitialization {

    public static void init(final DebugLevel debugLevel) {

        final ConsoleAppender console = new ConsoleAppender(); //create appender

        final String PATTERN = "[%p] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(debugLevel.associatedLog4JLevel);
        console.activateOptions();
        //add appender to any Logger (here is root)
        Logger.getRootLogger().addAppender(console);
        //repeat with all other desired appenders
    }



}
