package org.sagebionetworks.repo.util.github;

import org.springframework.http.HttpStatus;

public class GithubApiException extends RuntimeException {
	
	private HttpStatus status;
	private String responseBody;
	
	public GithubApiException(Throwable ex) {
		super(ex);
	}
	
	public GithubApiException(String message, HttpStatus status, String responseBody) {
		super(message);
		this.status = status;
		this.responseBody = responseBody;
	}
	
	public HttpStatus getStatus() {
		return status;
	}
	
	public String getResponseBody() {
		return responseBody;
	}

}
