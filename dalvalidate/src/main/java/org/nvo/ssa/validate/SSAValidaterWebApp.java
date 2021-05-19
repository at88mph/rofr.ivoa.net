package org.nvo.ssa.validate;

import org.nvo.service.validation.webapp.ValidaterWebApp;
import org.nvo.service.validation.webapp.ValidationSession;
import org.nvo.service.validation.webapp.ValidationException;
import org.nvo.service.validation.webapp.InternalServerException;

import java.io.IOException;
import java.io.FileNotFoundException;
import org.xml.sax.SAXException;

/**
 * an AJAX-capable web application that will validate SSA services
 */
public class SSAValidaterWebApp extends ValidaterWebApp {

    /**
     * create the web app
     */
    public SSAValidaterWebApp() { super(); }

    /**
     * a factory method for creating a new ValidationSession object.
     */
    protected ValidationSession newValidationSession() 
        throws ValidationException
    {
        try {
            return new SSAValidationSession();
        }
        catch (FileNotFoundException ex) {
            throw new InternalServerException(ex, 
                                              "Missing configuration file: " + 
                                              ex.getMessage());
        }
        catch (SAXException ex) {
            throw new InternalServerException(ex, "XML Syntax error in " +
                                              "configuration file: " + 
                                              ex.getMessage());
        }
        catch (IOException ex) {
            throw new InternalServerException(ex, "Trouble reading " +
                                              "configuration file: " + 
                                              ex.getMessage());
        }
    }

}