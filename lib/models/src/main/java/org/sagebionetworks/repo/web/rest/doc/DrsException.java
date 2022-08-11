package org.sagebionetworks.repo.web.rest.doc;

/**
 * Application exception indicates Drs Api has exception.
 *
 * @author sandhra
 */
public class DrsException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final Integer statusCode;
    private final String msg;

    /**
     * @param msg
     * @param statusCode
     */
    public DrsException(String msg, Integer statusCode) {
        super(msg);
        this.statusCode = statusCode;
        this.msg = msg;
    }

    /**
     * @param message
     * @param rootCause
     */
    public DrsException(String message, Throwable rootCause) {
        super(message, rootCause);
		this.statusCode = null;
		this.msg = message;

    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getMsg() {
        return msg;
    }
}
