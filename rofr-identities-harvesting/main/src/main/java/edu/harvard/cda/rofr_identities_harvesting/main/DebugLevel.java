package edu.harvard.cda.rofr_identities_harvesting.main;

import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Level;

public enum DebugLevel {

    OFF("off", Level.OFF)
    , FATAL("fatal", Level.FATAL)
    , ERROR("error", Level.ERROR)
    , WARN("warn", Level.WARN)
    , INFO("info", Level.INFO)
    , DEBUG("debug", Level.DEBUG)
    , TRACE("trace", Level.TRACE)
    , ALL("all", Level.ALL);

    public final String code;
    public final Level associatedLog4JLevel;

    private DebugLevel(final String code, final Level associatedLog4JLevel) {
        this.code = code;
        this.associatedLog4JLevel = associatedLog4JLevel;
    }

    public static DebugLevel fromCode(final String code) {
        for (final DebugLevel x: DebugLevel.values()) {
            if (x.code.equals(code))
                return x;
        }
        return null;
    }

    public static List<String> codes() {
        final List<String> rv = new ArrayList<>();
        for (final DebugLevel level: DebugLevel.values())
            rv.add(level.code);
        return rv;
    }
}
