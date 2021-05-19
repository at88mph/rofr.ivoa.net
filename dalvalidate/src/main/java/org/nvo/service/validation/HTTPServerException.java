package org.nvo.service.validation;

import java.io.IOException;

/**
 * an IOException indicating an error response received from a server
 */
public class HTTPServerException extends IOException {

    protected int code = -1;
    protected String rmessage = null;

    public HTTPServerException() {
        this("Unknown HTTP Server Error");
    }

    public HTTPServerException(String message) {
        super(message);
    }

    public HTTPServerException(int responseCode, String responseMessage) {
        super("HTTP Server Error: " + responseCode + " " + responseMessage);
        this.code = code;
        rmessage = responseMessage;
    }

    /**
     * return the HTTP Response code or -1 if it is unknown
     */
    public int getResponseCode() { return code; }

    /**
     * return the HTTP Response message or null if it is unknown
     */
    public String getResponseMessage() { return rmessage; }

}

