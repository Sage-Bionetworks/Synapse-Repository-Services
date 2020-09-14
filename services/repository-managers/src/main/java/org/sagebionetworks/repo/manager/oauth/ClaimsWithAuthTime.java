package org.sagebionetworks.repo.manager.oauth;

import java.util.Date;

import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

import io.jsonwebtoken.impl.DefaultClaims;

public class ClaimsWithAuthTime extends DefaultClaims {
	
	public static ClaimsWithAuthTime newClaims() {return new ClaimsWithAuthTime();}
	
	private ClaimsWithAuthTime() {}

	public ClaimsWithAuthTime setAuthTime(Date authTime) {
		setDate(OIDCClaimName.auth_time.name(), authTime);
		return this;
	}
	
	public Date getAuthTime() {
		return getDate(OIDCClaimName.auth_time.name());
	}

}
