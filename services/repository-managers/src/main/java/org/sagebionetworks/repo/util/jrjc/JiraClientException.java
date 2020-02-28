package org.sagebionetworks.repo.util.jrjc;

public class JiraClientException extends RuntimeException {

    public JiraClientException() {
        super();
    }

    public JiraClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public JiraClientException(String message) {
        super(message);
    }

    public JiraClientException(Throwable cause) {
        super(cause);
    }

}
