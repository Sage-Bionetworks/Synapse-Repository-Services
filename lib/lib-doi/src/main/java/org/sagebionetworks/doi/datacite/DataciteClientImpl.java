package org.sagebionetworks.doi.datacite;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientConfig;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class DataciteClientImpl implements DataciteClient {

	@Autowired
	private DataciteMetadataTranslator metadataTranslator;

	@Autowired
	private DataciteXmlTranslator xmlTranslator;

	private static final Integer TIME_OUT = 30 * 1000; // 30 seconds
	private static final String USER_AGENT = "Synapse";
	private String DATACITE_URL;
	private String USERNAME;
	private String PASSWORD;
	private final SimpleHttpClient client;

	public DataciteClientImpl(DataciteClientConfig config) {
		// Configure HTTP client for use
		SimpleHttpClientConfig httpClientConfig = new SimpleHttpClientConfig();
		httpClientConfig.setSocketTimeoutMs(TIME_OUT);
		client = new SimpleHttpClientImpl(httpClientConfig);
		USERNAME = config.getUsername();
		PASSWORD = config.getPassword();
		DATACITE_URL = "https://" + config.getDataciteDomain() + "/";
	}

	@Override
	public DataciteMetadata get(final String doiUri)
			throws IllegalArgumentException, NotFoundException, ServiceUnavailableException, NotReadyException {
		SimpleHttpRequest request = createRequest("metadata/", doiUri, null);
		SimpleHttpResponse response = null;
		try {
			response = client.get(request);
		} catch (IOException e) {
			throw new ServiceUnavailableException(e);
		}
		String xml = null;
		if (response.getStatusCode() == HttpStatus.SC_OK) {
			xml = response.getContent();
		} else {
			handleHttpErrorCode(response.getStatusCode());
		}
		return xmlTranslator.translate(xml);
	}

	@Override
	public void deactivate(final String doiUri)
			throws IllegalArgumentException, NotFoundException, ServiceUnavailableException, NotReadyException {
		SimpleHttpRequest request = createRequest("metadata/", doiUri, null);
		SimpleHttpResponse response = null;
		try {
			response = client.delete(request);
		} catch (IOException e) {
			throw new ServiceUnavailableException("Error occurred while issuing request to deactivate DOI", e);
		}
		if (response.getStatusCode() != HttpStatus.SC_OK) { // Successfully deactivated DOI
			handleHttpErrorCode(response.getStatusCode());
		}
	}

	@Override
	public void registerMetadata(final DataciteMetadata metadata, final String doiUri)
			throws IllegalArgumentException, NotFoundException, ServiceUnavailableException, NotReadyException {
		SimpleHttpRequest request = createRequest("metadata/", doiUri, "application/xml;charset=UTF-8");
		SimpleHttpResponse response = null;
		try {
			response = client.put(request, metadataTranslator.translate(metadata, doiUri));
		} catch (IOException e) {
			throw new ServiceUnavailableException("Error occurred while issuing request to register metadata", e);
		}
		if (response.getStatusCode() != HttpStatus.SC_CREATED) { // Successfully posted metadata
			handleHttpErrorCode(response.getStatusCode());
		}
	}

	@Override
	public void registerDoi(final String doiUri, final String url)
			throws IllegalArgumentException, NotFoundException, ServiceUnavailableException, NotReadyException {
		// Construct request to register the DOI and send it out
		SimpleHttpRequest request = createRequest("doi/", doiUri, "text/plain;charset=UTF-8");
		String content = registerDoiRequestBody(doiUri, url);
		SimpleHttpResponse response = null;
		try {
			response = client.put(request, content);
		} catch (IOException e) {
			throw new ServiceUnavailableException("Error occurred while issuing request to register DOI", e);
		}

		if (response.getStatusCode() != HttpStatus.SC_CREATED) { // Successfully created DOI
			handleHttpErrorCode(response.getStatusCode());
		}
	}

	/**
	 * Create a request to communicate with DataCite
	 *
	 * @param path        The path corresponding to the API call to make ("doi" or "metadata")
	 * @param contentType The content type to set in the header. If null, does not set a content type.
	 * @param doiUri      The URI to append to the API call. If null, does not append a URI
	 * @return
	 */
	SimpleHttpRequest createRequest(String path, String doiUri, String contentType) {
		SimpleHttpRequest request = new SimpleHttpRequest();
		if (doiUri == null) {
			request.setUri(DATACITE_URL + path);
		} else {
			request.setUri(DATACITE_URL + path + doiUri);
		}
		Map<String, String> headers = new HashMap<>();
		try {
			headers.put(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes("utf-8")));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Error encoding DataCite credentials");
		}
		if (contentType != null) {
			headers.put(HttpHeaders.CONTENT_TYPE, contentType);
		}
		headers.put(HttpHeaders.USER_AGENT, USER_AGENT);
		request.setHeaders(headers);
		return request;
	}

	static void handleHttpErrorCode(int code)
			throws IllegalArgumentException, NotFoundException, ServiceUnavailableException, NotReadyException {
		switch (code) {
			case HttpStatus.SC_BAD_REQUEST:
				throw new RuntimeException("Error registering metadata for DOI with DataCite. The request body was invalid, or the DOI prefix does not belong to Synapse.");
			case HttpStatus.SC_NO_CONTENT:
				throw new RuntimeException("DOI was not minted in DataCite or was not resolvable.");
			case HttpStatus.SC_NOT_FOUND:
				throw new NotFoundException("DOI was not found in DataCite");
			case HttpStatus.SC_UNAUTHORIZED:
				throw new RuntimeException("Error authenticating with DataCite.");
			case HttpStatus.SC_FORBIDDEN:
				throw new RuntimeException("Error accessing DOI. It may not belong to Synapse. There may have been an error authenticating with DataCite.");
			case HttpStatus.SC_PRECONDITION_FAILED:
				throw new NotFoundException("Metadata was not registered with DataCite. It may not have been registered, or may be propogating throughout their service.");
			case HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE:
				throw new ServiceUnavailableException("Error accessing DOI. The content type may not have been specified.");
			default:
				throw new ServiceUnavailableException("Received " + String.valueOf(code) + " error communicating with the DataCite DOI server.");
		}
	}

	static String registerDoiRequestBody(String doiUri, String url) {
		return "doi=" + doiUri + "\nurl=" + url;
	}
}
