package ncsa.xml.extractor;

/**
 * an exception signalling that a request to export a Node could not be 
 * satisfied.  
 */
public class ExportingNotReadyException extends Exception {
    private String name = null;
    private String type = null;

    /**
     * create an exception with a default message.
     */
    public ExportingNotReadyException() {
        super("Current node cannot be exported");
    }

    /**
     * create an exception with a given message.
     */
    public ExportingNotReadyException(String msg) {
        super(msg);
    }

    /**
     * create an exception with a given message.
     * @param type    the type of node that could not be extracted.
     * @param name    the name of the node that could not be extracted.
     * @param reason  the reason why the node could not be exported which will
     *                   be used as the exception message.
     */
    public ExportingNotReadyException(String type, String qname, String reason) 
    {
        super(reason);
        this.type = type;
        name = qname;
    }

    /**
     * return the name of the node that could not be exported.
     */
    public String getNodeName() {
        return name;
    }

    /**
     * return the type of the node that could not be exported.
     */
    public String getNodeType() {
        return type;
    }
}
