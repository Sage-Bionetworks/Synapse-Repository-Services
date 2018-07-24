package org.sagebionetworks.doi.datacite;

/*
 * Use to configure parameters in the DataCite client
 */
public class DataciteClientConfigImpl implements DataciteClientConfig {
	public void setUsername(String username){
		this.username = username;
	}

	public void setPassword(String password){
		this.password = password;
	}

	public void setDataciteDomain(String domain){
		this.dataciteDomain = domain;
	}

	public String getUsername(){
		return username;
	}

	public String getPassword(){
		return password;
	}

	public String getDataciteDomain(){
		return dataciteDomain;
	}

	private String username;
	private String password;
	private String dataciteDomain = "mds.datacite.org";
}
