package org.sagebionetworks.repo.model;

public class InviterAndPortalEndpoint {
	private String inviterPrincipalId;
	private String portalEndpoint;
	public InviterAndPortalEndpoint(String inviterPrincipalId,
			String portalEndpoint) {
		super();
		this.inviterPrincipalId = inviterPrincipalId;
		this.portalEndpoint = portalEndpoint;
	}
	public String getInviterPrincipalId() {
		return inviterPrincipalId;
	}
	public void setInviterPrincipalId(String inviterPrincipalId) {
		this.inviterPrincipalId = inviterPrincipalId;
	}
	public String getPortalEndpoint() {
		return portalEndpoint;
	}
	public void setPortalEndpoint(String portalEndpoint) {
		this.portalEndpoint = portalEndpoint;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((inviterPrincipalId == null) ? 0 : inviterPrincipalId
						.hashCode());
		result = prime * result
				+ ((portalEndpoint == null) ? 0 : portalEndpoint.hashCode());
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
		InviterAndPortalEndpoint other = (InviterAndPortalEndpoint) obj;
		if (inviterPrincipalId == null) {
			if (other.inviterPrincipalId != null)
				return false;
		} else if (!inviterPrincipalId.equals(other.inviterPrincipalId))
			return false;
		if (portalEndpoint == null) {
			if (other.portalEndpoint != null)
				return false;
		} else if (!portalEndpoint.equals(other.portalEndpoint))
			return false;
		return true;
	}
	
	

}
