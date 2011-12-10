/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author deflaux
 *
 */
public class ServletTestHelperException extends RuntimeException {
	
	private final MockHttpServletResponse response;
	private static final EntityObjectMapper objectMapper = new EntityObjectMapper();
	
	public ServletTestHelperException(MockHttpServletResponse response) {
		super(response.getErrorMessage());
		this.response = response;
	}
	
	public int getHttpStatus() {
		return response.getStatus();
	}
	
	public String getHttpErrorMessage() {
		return response.getErrorMessage();
	}
	
	public String getServiceErrorMessage() throws JsonParseException, JsonMappingException, UnsupportedEncodingException, IOException {
		ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
		return errorResponse.getReason();
	}
}
