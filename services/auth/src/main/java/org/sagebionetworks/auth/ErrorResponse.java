package org.sagebionetworks.auth;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Container for any error info we want to return to clients of this service.
 * <p>
 * 
 * For any non-20x HTTP status code, clients should expect to deserialize this
 * instead of a model object.
 * 
 * @author deflaux
 */
//
// TODO This is a COPY of org.sagebionetworks.repo.model.ErrorResponse
// need to factor it out into a 'common' place
//
@XmlRootElement
public class ErrorResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	private String reason;

	/**
	 * Default constructor
	 */
	public ErrorResponse() {
	}

	/**
	 * @param reason
	 */
	public ErrorResponse(String reason) {
		this.reason = reason;
	}

	/**
	 * @return The reason for the error
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * @param reason
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}
}
