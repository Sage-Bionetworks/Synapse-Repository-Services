package org.sagebionetworks.doi.datacite;

/**
 * Used to configure parameters in the DataCite client
 */
public interface DataciteClientConfig {

	/**
	 * Get the username used to authenticate with DataCite.
	 */
	String getUsername();

	/**
	 * Get the password used to authenticate with DataCite
	 */
	String getPassword();

	/**
	 * Get the domain for the DataCite API. Defaults to "mds.datacite.org"
	 */
	String getDataciteDomain();
}
