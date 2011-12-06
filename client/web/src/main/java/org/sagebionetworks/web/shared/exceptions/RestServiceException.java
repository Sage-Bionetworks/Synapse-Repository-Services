package org.sagebionetworks.web.shared.exceptions;

import com.google.gwt.user.client.rpc.IsSerializable;

public class RestServiceException extends Exception implements IsSerializable {
	private String message;
		
	public RestServiceException() {
	}

	public RestServiceException(String message) {
		this.message = message;		
	}

	public String getMessage() {
		return message;
	}
	
}
