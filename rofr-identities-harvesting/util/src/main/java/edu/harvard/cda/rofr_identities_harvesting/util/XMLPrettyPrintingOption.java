package edu.harvard.cda.rofr_identities_harvesting.util;

public enum XMLPrettyPrintingOption {
    OFF((Integer)null), SPACE_0(0), SPACE_1(1), SPACE_2(2), SPACE_3(3)
                      , SPACE_4(4), SPACE_5(5), SPACE_6(6), SPACE_7(7), SPACE_8(8);

    private final Integer indentAmount;

    private XMLPrettyPrintingOption(final Integer indentAmount) {
        this.indentAmount = indentAmount;
    }

    public int getIndentAmount() {
        if (this==OFF) throw new IllegalStateException();
        return indentAmount;
    }
}
