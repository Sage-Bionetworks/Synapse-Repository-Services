package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PrincipalLookupResults {
	private Collection<String> principalIds = null;
	private List<String> unmatchedEmails = null;
	
	public PrincipalLookupResults(Collection<String> principalIds,
			List<String> unmatchedEmails) {
		super();
		this.principalIds = principalIds;
		this.unmatchedEmails = unmatchedEmails;
	}

	public Collection<String> getPrincipalIds() {
		return principalIds;
	}

	public void setPrincipalIds(Collection<String> principalIds) {
		this.principalIds = principalIds;
	}

	public List<String> getUnmatchedEmails() {
		return unmatchedEmails;
	}

	public void setUnmatchedEmails(List<String> unmatchedEmails) {
		this.unmatchedEmails = unmatchedEmails;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((principalIds == null) ? 0 : principalIds.hashCode());
		result = prime * result
				+ ((unmatchedEmails == null) ? 0 : unmatchedEmails.hashCode());
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
		if (unmatchedEmails == null) {
			if (other.unmatchedEmails != null)
				return false;
		} else if (!unmatchedEmails.equals(other.unmatchedEmails))
			return false;
		return true;
	}

}
