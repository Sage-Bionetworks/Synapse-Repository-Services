package org.sagebionetworks.client.exceptions;

import org.sagebionetworks.repo.model.auth.TwoFactorAuthErrorResponse;

public class SynapseTwoFactorAuthRequiredException extends SynapseServerException {

	private Long userId;
	private String twoFaToken;
	
	public SynapseTwoFactorAuthRequiredException(TwoFactorAuthErrorResponse errorResponse) {
		super(errorResponse.getReason(), errorResponse.getErrorCode());
		this.userId = errorResponse.getUserId();
		this.twoFaToken = errorResponse.getTwoFaToken();
	}
	
	public Long getUserId() {
		return userId;
	}
	
	public String getTwoFaToken() {
		return twoFaToken;
	}

}
