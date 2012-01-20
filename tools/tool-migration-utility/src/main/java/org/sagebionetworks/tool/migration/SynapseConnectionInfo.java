package org.sagebionetworks.tool.migration;

/**
 * Holds all of the information needed to establish a Synapse Connection
 * @author John
 *
 */
public class SynapseConnectionInfo {
	
	/**
	 * The only public constructor.
	 * @param authenticationEndPoint
	 * @param repositoryEndPoint
	 * @param adminUsername
	 * @param adminPassword
	 */
	public SynapseConnectionInfo(String authenticationEndPoint,
			String repositoryEndPoint, String adminUsername,
			String adminPassword) {
		super();
		this.authenticationEndPoint = authenticationEndPoint;
		this.repositoryEndPoint = repositoryEndPoint;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
	}
	
	private String authenticationEndPoint;
	private String repositoryEndPoint;
	private String adminUsername;
	private String adminPassword;
	
	public String getAuthenticationEndPoint() {
		return authenticationEndPoint;
	}
	public String getRepositoryEndPoint() {
		return repositoryEndPoint;
	}
	public String getAdminUsername() {
		return adminUsername;
	}
	public String getAdminPassword() {
		return adminPassword;
	}
	@Override
	public String toString() {
		return "SynapseConnectionInfo [authenticationEndPoint="
				+ authenticationEndPoint + ", repositoryEndPoint="
				+ repositoryEndPoint + ", adminUsername=" + adminUsername + "]";
	}

}
