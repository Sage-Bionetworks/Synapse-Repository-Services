/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.ErrorResponse;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author deflaux
 *
 */
public class ServletTestHelperException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private int httpStatus = -1;
	private String message;
	private static final EntityObjectMapper objectMapper = new EntityObjectMapper();
	
	/**
	 * @param response
	 */
	public ServletTestHelperException(MockHttpServletResponse response) {
		this.message = "caught ServletTestHelperException";
		this.httpStatus = response.getStatus();
		try {
			ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
			this.message = errorResponse.getReason();
		} catch (Exception e) {
			// This is okay to swallow because its just a best effort to
			// retrieve more info when we are already in an exception situation
			this.message = response.getErrorMessage();
		}
	}
	
	/**
	 * @return HTTP status code
	 */
	public int getHttpStatus() {
		return httpStatus;
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getLocalizedMessage()
	 */
	@Override
	public String getLocalizedMessage() {
		return message;
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return message;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ServletTestHelperException [httpStatus=" + httpStatus
				+ ", message=" + message + ", toString()=" + super.toString()
				+ "]";
	}
}
