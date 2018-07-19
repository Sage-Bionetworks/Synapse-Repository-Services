package org.sagebionetworks.doi.datacite;

/*
 * Use to configure parameters in the DataCite client
 */
public class DataciteClientConfig {
	public void setUsername(String username){
		this.username = username;
	}

	public void setPassword(String password){
		this.password = password;
	}

	public void setDataciteUrl(String dataciteUrl){
		this.dataciteUrl = dataciteUrl;
	}

	public String getUsername(){
		return username;
	}

	public String getPassword(){
		return password;
	}

	public String getDataciteUrl(){
		return dataciteUrl;
	}

	private String username;
	private String password;
	private String dataciteUrl;
}
