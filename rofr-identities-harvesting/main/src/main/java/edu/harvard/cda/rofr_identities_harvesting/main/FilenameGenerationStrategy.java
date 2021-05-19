package edu.harvard.cda.rofr_identities_harvesting.main;

import java.util.List;
import java.util.ArrayList;

public enum FilenameGenerationStrategy {

    ROFR("rofr", "this filename generator strategy produces filenames according to the convention used in the RofR application"
         +". E.g. it maps 'ivo://a.b.c/d/e' to 'a.b.c_d_e'")
    , ALT1("alt1", "this is an alternative filename generation strategy. E.g. it maps ivo://a.b.c/d/e to ivo_a_b_c_d_e");

    private String code;
    private String msg; 

    private FilenameGenerationStrategy(final String code, final String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return code;
    }

    public static FilenameGenerationStrategy fromCode(final String code) {

        for (final FilenameGenerationStrategy fgs: FilenameGenerationStrategy.values())
            if (fgs.code.equals(code))
                return fgs;
        return null;
    }

    public static List<String> codes() {
        final List<String> rv = new ArrayList<>();
        for (final FilenameGenerationStrategy fgs: FilenameGenerationStrategy.values())
            rv.add(fgs.code);
        return rv;
    }
}
