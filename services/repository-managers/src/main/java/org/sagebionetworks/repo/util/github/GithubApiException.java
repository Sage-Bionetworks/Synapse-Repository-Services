package org.sagebionetworks.repo.util.github;

public class GithubApiException extends RuntimeException {
	
	public GithubApiException(Throwable ex) {
		super(ex);
	}
	
	public GithubApiException(String message) {
		super(message);
	}

}
