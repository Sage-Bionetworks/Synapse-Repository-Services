package org.sagebionetworks.repo.manager.oauth;

import java.util.Date;

import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

import io.jsonwebtoken.impl.DefaultClaims;

public class ClaimsWithAuthTime extends DefaultClaims {
	
	public static ClaimsWithAuthTime newClaims() {return new ClaimsWithAuthTime();}
	
	private ClaimsWithAuthTime() {}

	public ClaimsWithAuthTime setAuthTime(Date authTime) {
		setDateAsSeconds(OIDCClaimName.auth_time.name(), authTime);
		return this;
	}
	
	public Date getAuthTime() {
		Object value = get(OIDCClaimName.auth_time.name());
		if (value == null) {
			return null;
		}
		// The default implementation of get(name, Date) will not convert from seconds
		// for "non-standard" claims so we have to use the "toSpecDate" ourselves
		return toSpecDate(value, OIDCClaimName.auth_time.name());
	}

}
