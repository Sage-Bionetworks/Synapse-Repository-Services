package org.sagebionetworks.repo.web;

public class TwoFactorAuthRequiredException extends RuntimeException {

	public static final String ERROR_MESSAGE = "Two factor authentication required.";
	
	private Long userId;
	private String twoFaToken;
	
	public TwoFactorAuthRequiredException(Long userId, String twoFaToken) {
		super(ERROR_MESSAGE);
		this.userId = userId;
		this.twoFaToken = twoFaToken;
	}
	
	public Long getUserId() {
		return userId;
	}
	
	public String getTwoFaToken() {
		return twoFaToken;
	}

}
