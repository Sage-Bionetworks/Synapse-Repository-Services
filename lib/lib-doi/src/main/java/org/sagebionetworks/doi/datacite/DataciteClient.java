package org.sagebionetworks.doi.datacite;

import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

public interface DataciteClient {

	/**
	 * Retrieves the metadata stored in DataCite for a specific DOI.
	 * @param doiUri The URI of the DOI to retrieve metadata. e.g. "10.1234/abc123"
	 * @return The metadata of the object stored in DataCite
	 * @throws ServiceUnavailableException
	 */
	DataciteMetadata get(String doiUri)
			throws IllegalArgumentException, NotFoundException, ServiceUnavailableException, NotReadyException;

	/**
	 * Registers a URL with DataCite; only works if metadata exists. Idempotent.
	 * Can also be used to point an existing DOI to a new URL.
	 * @param doiUri The URI of the DOI. e.g. "10.1234/abc123"
	 * @param url The URL to which the DOI should resolve
	 * @throws ServiceUnavailableException An issue was encountered preventing communication with DataCite.
	 * @throws NotReadyException The register call was made before the metadata was registered and propogated
	 * through DataCite's service
	 */
	void registerDoi(final String doiUri, final String url)
			throws IllegalArgumentException, NotFoundException, ServiceUnavailableException, NotReadyException;

	/**
	 * Registers metadata with DataCite. Idempotent.
	 * @param metadata The metadata to register on DataCite
	 * @param doiUri The URI of the DOI. e.g. "10.1234/abc123"
	 * @throws ServiceUnavailableException
	 */
	void registerMetadata(final DataciteMetadata metadata, final String doiUri)
			throws IllegalArgumentException, NotFoundException, ServiceUnavailableException, NotReadyException;

	/**
	 * Mark the DOI as 'inactive' in DataCite. This does not delete it, but it removes it from DataCite indexes and
	 * catalogues, and the associated information about the object can only be found with the DOI itself.
	 * Can be undone by registering the DOI with the appropriate URL.
	 * @param doiUri The URI to deactivate.
	 * @throws ServiceUnavailableException
	 */
	void deactivate(final String doiUri)
			throws IllegalArgumentException, NotFoundException, ServiceUnavailableException, NotReadyException;
}
