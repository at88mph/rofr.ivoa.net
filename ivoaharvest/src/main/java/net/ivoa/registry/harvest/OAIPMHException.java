package net.ivoa.registry.harvest;

/**
 * an exception indicating that an OAI-PMH service error was read from 
 * the harvesting response.
 */
public class OAIPMHException extends HarvestingException {
    String code = null;
    String oaimsg = null;

    /**
     * create the exception
     * @param code     the OAI-PMH-defined name for the error detected
     * @param message  the explanatory message supplied by the service
     * @param goodCount  the number of successfully harvested records prior 
     *                      to this failure.  A negative number indicates that 
     *                      the number is not known.
     */
    public OAIPMHException(String code, String message, int goodCount) {
        super("OAI-PMH "+code+" error: "+message, goodCount);
    }

    /**
     * create the exception.  This constructor will assume that no records
     * have been harvested (i.e. it occurred while not using a resumption
     * token).  
     * @param code     the OAI-PMH-defined name for the error detected
     * @param message  the explanatory message supplied by the service
     */
    public OAIPMHException(String code, String message) {
        this(code, message, 0);
    }

    /**
     * return the OAI-PMH-defined name for the error
     */
    public String getCode() { return code; }

    /**
     * return the explanatory message provided by the service
     */
    public String getServiceMessage() { return oaimsg; } 
    
}