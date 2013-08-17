package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Simple wrapper that exposes the response code and error message. From: http://stackoverflow.com/questions/1302072/how-can-i-get-the-http-status-code-out-of-a-servletresponse-in-a-servletfilter
 * 
 *
 */
public class StatusExposingServletResponse extends HttpServletResponseWrapper {

	public StatusExposingServletResponse(HttpServletResponse response) {
		super(response);
	}

    private int httpStatus;
    private String errorMessage;

    @Override
    public void sendError(int sc) throws IOException {
        httpStatus = sc;
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        httpStatus = sc;
        errorMessage = msg;
        super.sendError(sc, msg);
    }

    @Override
    public void setStatus(int sc) {
        httpStatus = sc;
        super.setStatus(sc);
    }

    /**
     * Get the status
     * @return
     */
    public int getStatus() {
        return httpStatus;
    }
    
    /**
     * Get the error message.
     * @return
     */
    public String getErrorMessage(){
    	return errorMessage;
    }
}
