package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.Date;

public class DBOCredentialBackup {
	private Long principalId;
 	private Date validatedOn;
 	private String sessionToken;
 	private String passHash;
 	private String secretKey;
 	private Boolean agreesToTermsOfUse;	
 	
 	public DBOCredentialBackup() {
 	}
 	
 	public Long getPrincipalId() {
		return principalId;
	}
	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}
	public Date getValidatedOn() {
		return validatedOn;
	}
	public void setValidatedOn(Date validatedOn) {
		this.validatedOn = validatedOn;
	}
	public String getSessionToken() {
		return sessionToken;
	}
	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}
	public String getPassHash() {
		return passHash;
	}
	public void setPassHash(String passHash) {
		this.passHash = passHash;
	}
	public String getSecretKey() {
		return secretKey;
	}
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	public Boolean getAgreesToTermsOfUse() {
		return agreesToTermsOfUse;
	}
	public void setAgreesToTermsOfUse(Boolean agreesToTermsOfUse) {
		this.agreesToTermsOfUse = agreesToTermsOfUse;
	}

}
