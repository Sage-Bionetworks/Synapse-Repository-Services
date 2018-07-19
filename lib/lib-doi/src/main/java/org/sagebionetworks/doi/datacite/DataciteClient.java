package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

public interface DataciteClient {

	/*
	 * Gets the metadata for a DOI association
	 */
	DataciteMetadata get(String doiUri) throws ServiceUnavailableException;

	/*
	 * Registers a new DOI object with metadata to the DOI provider
	 */
	void create(DataciteMetadata metadata, String doiUri, String url) throws ServiceUnavailableException;

	/*
	 * Alter the metadata for a resource that already has a DOI. Does not change URL referred to by the DOI.
	 */
	void update(DataciteMetadata metadata, String doiUri) throws ServiceUnavailableException;

	/*
	 * Mark the DOI as 'inactive'. This does not delete it, but it removes it from DataCite indexes and
	 * catalogues and can only be found with the DOI itself.
	 */
	void deactivate(final String doiUri) throws ServiceUnavailableException;

	/*
	 * Set the configuration for the DataCite client
	 */
	void setConfig(DataciteClientConfig config);
}
