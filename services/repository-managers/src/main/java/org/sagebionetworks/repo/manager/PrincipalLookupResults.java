package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;

public class PrincipalLookupResults {
	private Collection<String> principalIds = null;
	private List<String> invalidEmails = null;
	
	public PrincipalLookupResults(Collection<String> principalIds,
			List<String> invalidEmails) {
		super();
		this.principalIds = principalIds;
		this.invalidEmails = invalidEmails;
	}

	public Collection<String> getPrincipalIds() {
		return principalIds;
	}

	public void setPrincipalIds(Collection<String> principalIds) {
		this.principalIds = principalIds;
	}

	public List<String> getInvalidEmails() {
		return invalidEmails;
	}

	public void setInvalidEmails(List<String> invalidEmails) {
		this.invalidEmails = invalidEmails;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((principalIds == null) ? 0 : principalIds.hashCode());
		result = prime * result
				+ ((invalidEmails == null) ? 0 : invalidEmails.hashCode());
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
		PrincipalLookupResults other = (PrincipalLookupResults) obj;
		if (principalIds == null) {
			if (other.principalIds != null)
				return false;
		} else if (!principalIds.equals(other.principalIds))
			return false;
		if (invalidEmails == null) {
			if (other.invalidEmails != null)
				return false;
		} else if (!invalidEmails.equals(other.invalidEmails))
			return false;
		return true;
	}

}
