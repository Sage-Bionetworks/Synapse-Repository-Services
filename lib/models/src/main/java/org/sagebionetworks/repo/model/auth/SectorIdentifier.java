package org.sagebionetworks.repo.model.auth;

/*
 * Contains information for an OIDC Sector Identifer, as defined by
 * https://openid.net/specs/openid-connect-registration-1_0.html#SectorIdentifierValidation
 */
public class SectorIdentifier {
	/*
	 * The URI for the Sector Identifier
	 */
	private String sectorIdentifierUri;
	
	/*
	 * The 'salt' used when hashing or encrypting the sector identifier and Synapse user id into a 'paired subject id'.  See:
	 * https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg
	 */
	private String salt;

	public String getSectorIdentifierUri() {
		return sectorIdentifierUri;
	}

	public void setSectorIdentifierUri(String sectorIdentifierUri) {
		this.sectorIdentifierUri = sectorIdentifierUri;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((salt == null) ? 0 : salt.hashCode());
		result = prime * result + ((sectorIdentifierUri == null) ? 0 : sectorIdentifierUri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SectorIdentifier other = (SectorIdentifier) obj;
		if (salt == null) {
			if (other.salt != null)
				return false;
		} else if (!salt.equals(other.salt))
			return false;
		if (sectorIdentifierUri == null) {
			if (other.sectorIdentifierUri != null)
				return false;
		} else if (!sectorIdentifierUri.equals(other.sectorIdentifierUri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SectorIdentifier [sectorIdentifierUri=" + sectorIdentifierUri + ", salt=" + salt + "]";
	}
	
	

}
