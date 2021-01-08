package org.sagebionetworks.repo.manager.stack;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

@Service("prodDetector")
public class ProdDetectorImpl implements ProdDetector {

	static final String VERSION_INFO_ENDPOINT = "/version";
	
	static final String FAILED_REQUEST_MSG = "Could not fetch prod version info from {}: {}";
	
	static final String USER_AGENT_TEMPLATE = "SynapseRepositoryStack/%s";

	private final SimpleHttpClient httpClient;
	private final StackConfiguration stackConfiguration;
	private final LoggerProvider logProvider;
	
	private SimpleHttpRequest versionInfoRequest;
	private String currentStackInstance;
	private Logger log;

	@Autowired
	public ProdDetectorImpl(final SimpleHttpClient httpClient, final StackConfiguration stackConfiguration, final LoggerProvider logProvider) {
		this.httpClient = httpClient;
		this.stackConfiguration = stackConfiguration;
		this.logProvider = logProvider;
	}
	
	@PostConstruct
	protected void init() {
		this.log = logProvider.getLogger(ProdDetectorImpl.class.getName());
		this.currentStackInstance = stackConfiguration.getStackInstance();
		
		this.versionInfoRequest = new SimpleHttpRequest();
		this.versionInfoRequest.setUri(stackConfiguration.getRepositoryServiceProdEndpoint() + VERSION_INFO_ENDPOINT);

		Map<String, String> headers = ImmutableMap.of(
				HttpHeaders.USER_AGENT, String.format(USER_AGENT_TEMPLATE, currentStackInstance),
				HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType(),
				// Force revalidation along the way to avoid caching issues
				HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue()
		);

		this.versionInfoRequest.setHeaders(headers);
	}

	@Override
	public Optional<Boolean> isProductionStack() {
		Optional<SynapseVersionInfo> response = this.sendVersionRequest()
				.flatMap(this::parseVersionResponse);

		return response.flatMap(versionInfo -> 
			Optional.of(currentStackInstance.equals(versionInfo.getStackInstance()))
		);

	}

	Optional<SimpleHttpResponse> sendVersionRequest() {
		SimpleHttpResponse response;

		try {
			response = httpClient.get(versionInfoRequest);
		} catch (IOException e) {
			return failedRequest(e.getMessage(), null, e);
		}

		return Optional.of(response);
	}

	Optional<SynapseVersionInfo> parseVersionResponse(SimpleHttpResponse response) {
		if (response.getStatusCode() != HttpStatus.SC_OK) {
			String message = StringUtils.isBlank(response.getStatusReason()) ? response.getContent() : response.getStatusReason();
			return failedRequest(message, response, null);
		}

		if (StringUtils.isBlank(response.getContent())) {
			return failedRequest("Respose body was empty", response, null);
		}

		final SynapseVersionInfo versionInfo;

		try {
			JSONObjectAdapter responseAdapter = new JSONObjectAdapterImpl(response.getContent());
			versionInfo = new SynapseVersionInfo(responseAdapter);
		} catch (JSONObjectAdapterException e) {
			return failedRequest(e.getMessage(), response, e);
		}

		return Optional.of(versionInfo);

	}
	
	<T> Optional<T> failedRequest(String message, SimpleHttpResponse response, Exception e) {
		StringBuilder errorMessage = new StringBuilder(FAILED_REQUEST_MSG);
		
		if (response != null) {
			errorMessage
				.append(" (Status Code: ")
				.append(response.getStatusCode())
				.append(")");
		}
		
		log.error(errorMessage.toString(), versionInfoRequest.getUri(), message, e);
		
		return Optional.empty();
	}

}
