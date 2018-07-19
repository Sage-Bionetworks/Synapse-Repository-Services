package org.sagebionetworks.doi.datacite;


import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.simpleHttpClient.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataciteClientImpl implements DataciteClient {

	@Autowired
	private DataciteMetadataTranslator metadataTranslator;

	@Autowired
	private DataciteXmlTranslator xmlTranslator;

	private static final Integer TIME_OUT = 9000; // 9 seconds
	private static final String USER_AGENT = "Synapse";
	private String DATACITE_URL;
	private String USERNAME;
	private String PASSWORD;
	private final SimpleHttpClient client;

	public DataciteClientImpl() {
		// Configure HTTP client for use
		SimpleHttpClientConfig httpClientConfig = new SimpleHttpClientConfig();
		httpClientConfig.setSocketTimeoutMs(TIME_OUT);
		client = new SimpleHttpClientImpl(httpClientConfig);
	}

	public void setConfig(DataciteClientConfig config) {
		USERNAME = config.getUsername();
		PASSWORD = config.getPassword();
		DATACITE_URL = config.getDataciteUrl();
	}

	@Override
	public DataciteMetadata get(final String doiUri) throws ServiceUnavailableException {
		// Call Datacite API to get metadata
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(DATACITE_URL + "metadata/" + doiUri);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.AUTHORIZATION, USERNAME + ":" + PASSWORD);
		headers.put(HttpHeaders.USER_AGENT, USER_AGENT);
		request.setHeaders(headers);

		SimpleHttpResponse response = null;
		try {
			response = client.get(request);
		} catch (IOException e) {
			throw new ServiceUnavailableException(e);
		}

		// See Datacite MDS support docs: https://support.datacite.org/docs/mds-api-guide#section-getting-started
		// Outlines all possible HTTP response status codes
		String xml = null;
		if (response.getStatusCode() == HttpStatus.SC_OK) { // Success
			xml = response.getContent();
		} else if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) { // Not minted or not resolvable
			throw new NotFoundException("DOI " + doiUri + " was not minted in DataCite or was not resolvable.");
		} else if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) { // No login - this is an issue with us or DataCite
			throw new ServiceUnavailableException("Error authenticating with DataCite.");
		} else if (response.getStatusCode() == HttpStatus.SC_FORBIDDEN) { // Login problem or data belongs to another party
			throw new NotFoundException("Error getting metadata for DOI: " + doiUri + " may not belong to Synapse. There may have been an error authenticating with DataCite.");
		} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) { // DOI does not exist in DataCite database
			throw new NotFoundException("DOI " + doiUri + " was not found in DataCite.");
		} else if (response.getStatusCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
			throw new ServiceUnavailableException("Received " + String.valueOf(response.getStatusCode()) + " error communicating with the DataCite DOI server.");
		}

		// Translate XML to our DOI Object and pass it
		return xmlTranslator.translate(xml);
	}

	/*
	 * Registers a new DOI object with metadata to the DOI provider
	 */
	@Override
	public void create(final DataciteMetadata metadata, final String doiUri, final String url) throws ServiceUnavailableException {
		registerMetadata(metadata, doiUri);
		registerDoi(doiUri, url);
	}

	/*
	 * Alter the metadata for a resource that already has a DOI. Does not change URL referred to by the DOI.
	 */
	@Override
	public void update(final DataciteMetadata metadata, final String doiUri) throws ServiceUnavailableException {
		registerMetadata(metadata, doiUri);
	}

	/*
	 * Mark the DOI as 'inactive'. This does not delete it, but it removes it from DataCite indexes and
	 * catalogues and can only be found with the DOI itself.
	 */
	@Override
	public void deactivate(final String doiUri) throws ServiceUnavailableException {
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(DATACITE_URL + "metadata/" + doiUri);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.AUTHORIZATION, USERNAME + ":" + PASSWORD);
		headers.put(HttpHeaders.USER_AGENT, USER_AGENT);
		request.setHeaders(headers);

		SimpleHttpResponse response = null;
		try {
			response = client.delete(request);
		} catch (IOException e) {
			throw new ServiceUnavailableException("Error occurred while issuing request to deactivate DOI", e);
		}

		if (response.getStatusCode() == HttpStatus.SC_OK) { // Successfully deactivated DOI

		} else if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) { // No login
			throw new ServiceUnavailableException("Error authenticating with DataCite.");
		} else if (response.getStatusCode() == HttpStatus.SC_FORBIDDEN) { // Login problem or quota exceeded
			throw new ServiceUnavailableException("Error deactivating DOI: " + doiUri + ". There may have been an error authenticating with DataCite, or the DOI may not belong to Synapse.");
		} else if (response.getStatusCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
			throw new ServiceUnavailableException("Received " + String.valueOf(response.getStatusCode()) + " error communicating with the DataCite DOI server.");
		}
	}

	/*
	 * Registers metadata with the DOI provider
	 */
	void registerMetadata(final DataciteMetadata metadata, final String doiUri) throws ServiceUnavailableException {
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(DATACITE_URL + "metadata/");
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.AUTHORIZATION, USERNAME + ":" + PASSWORD);
		headers.put(HttpHeaders.CONTENT_TYPE, "application/xml;charset=UTF-8");
		headers.put(HttpHeaders.USER_AGENT, USER_AGENT);
		request.setHeaders(headers);

		SimpleHttpResponse response = null;
		try {
			response = client.post(request, metadataTranslator.translate(metadata, doiUri));
		} catch (IOException e) {
			throw new ServiceUnavailableException("Error occurred while issuing request to register metadata", e);
		}

		if (response.getStatusCode() == HttpStatus.SC_CREATED) { // Successfully posted metadata

		} else if (response.getStatusCode() == HttpStatus.SC_BAD_REQUEST) { // Invalid XML or wrong prefix
			throw new IllegalArgumentException("Error registering metadata for DOI: " + doiUri + " with DataCite. The XML was invalid, or the prefix does not belong to Synapse.");
		} else if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) { // No login
			throw new ServiceUnavailableException("Error authenticating with DataCite.");
		} else if (response.getStatusCode() == HttpStatus.SC_FORBIDDEN) { // Login problem or quota exceeded
			throw new ServiceUnavailableException("Error registering metadata for DOI: " + doiUri + ". There may have been an error authenticating with DataCite, or the DOI may not belong to Synapse.");
		} else if (response.getStatusCode() == HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE) { // Excluded content type in header
			throw new IllegalArgumentException("Error registering metadata for DOI: " + doiUri + " with DataCite. The content type may not have been specified.");
		} else if (response.getStatusCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
			throw new ServiceUnavailableException("Received " + String.valueOf(response.getStatusCode()) + " error communicating with the DataCite DOI server.");
		}
	}

	/*
	 * Registers a URL with the DOI provider; only works if metadata exists.
	 * Can also be used to point an existing DOI to a new URL
	 *
	 * Extracted to private method because it may have utility in case we ever need to change the URL
	 * of DOIs without touching metadata.
	 */
	 void registerDoi(final String doiUri, final String url) throws ServiceUnavailableException {
		// Construct request to register the DOI and send it out
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(DATACITE_URL + "doi/" + doiUri);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.AUTHORIZATION, USERNAME + ":" + PASSWORD);
		headers.put(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
		headers.put(HttpHeaders.USER_AGENT, USER_AGENT);
		request.setHeaders(headers);

		String content = registerDoiRequestBody(doiUri, url);

		SimpleHttpResponse response = null;
	 	try {
	 		response = client.put(request, content);
		} catch (IOException e) {
			throw new ServiceUnavailableException("Error occurred while issuing request to register DOI", e);
		}

		 if (response.getStatusCode() == HttpStatus.SC_CREATED) { // Successfully created DOI

		 } else if (response.getStatusCode() == HttpStatus.SC_BAD_REQUEST) { // Request body not exactly two lines/wrong domain/wrong prefix
			 throw new IllegalArgumentException("Error creating DOI: " + doiUri + ". The request body was invalid, or the DOI prefix was wrong.");
		 } else if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) { // No login
			 throw new ServiceUnavailableException("Error authenticating with DataCite.");
		 } else if (response.getStatusCode() == HttpStatus.SC_FORBIDDEN) { // Login problem or quota exceeded
			 throw new ServiceUnavailableException("Error creating DOI: " + doiUri + ". There may have been an error authenticating with DataCite, or the DOI may not belong to Synapse.");
		 } else if (response.getStatusCode() == HttpStatus.SC_PRECONDITION_FAILED) { // Metadata not previously uploaded
			throw new ForbiddenException("Cannot create DOI: " + doiUri + " because the metadata was not previously registered.");
		 } else if (response.getStatusCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
			 throw new ServiceUnavailableException("Received " + String.valueOf(response.getStatusCode()) + " error communicating with the DataCite DOI server.");
		 }
	}

	static String registerDoiRequestBody(String doiUri, String url) {
		return "doi=" + doiUri + "\nurl=" + url;
	}
}
