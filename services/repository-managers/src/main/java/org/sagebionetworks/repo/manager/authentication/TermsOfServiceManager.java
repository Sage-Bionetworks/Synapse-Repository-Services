package org.sagebionetworks.repo.manager.authentication;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.util.Clock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

@Service
public class TermsOfServiceManager {
	
	private static final Logger LOGGER = LogManager.getLogger(TermsOfServiceManager.class);	
	
	private static final String DEFAULT_VERSION = "0.0.0";
	private static final Date DEFAULT_REQUIREMENT_DATE = Date.from(Instant.parse("2011-01-01T00:00:00.000Z"));

	private static final String VERSION_LATEST = "latest";
	
	private static final Duration VERSION_CACHE_EXPIRATION = Duration.of(10, ChronoUnit.MINUTES);
	
	private static final String TOS_REPO = "Sage-Bionetworks/Sage-Governance-Documents";
	private static final String TOS_API_VERSION = "https://api.github.com/repos/" + TOS_REPO + "/releases/%s";
	private static final String TOS_URL_FORMAT = "https://raw.githubusercontent.com/" + TOS_REPO + "/refs/tags/%s/Terms.md";
	
	private static final String[] GITHUB_REQUEST_HEADERS = new String[] {
		HttpHeaders.USER_AGENT, "Synapse-App",
		HttpHeaders.ACCEPT, "application/vnd.github+json",
		"X-GitHub-Api-Version", "2022-11-28"
	};
	
	private static final String PROPERTY_RELEASE_TAG = "tag_name";

	private static final String MESSAGE_VERSION_UNAVAILABLE = "The {} terms of service version is unavailable.";
	
	private HttpClient httpClient;
	private LoadingCache<String, String> versionCache;
	
	public TermsOfServiceManager(HttpClient defaultHttpClient, Clock clock) {
		this.httpClient = defaultHttpClient;
		this.versionCache = CacheBuilder.newBuilder()
			.ticker(new Ticker() {
				
				@Override
				public long read() {
					return clock.nanoTime();
				}
			})
			.expireAfterWrite(VERSION_CACHE_EXPIRATION)
			.build(CacheLoader.from(this::fetchTermsOfServiceVersion));
	}

	/**
	 * @return The information about the current terms of service and requirements
	 */
	public TermsOfServiceInfo getTermsOfUseInfo() {
		TermsOfServiceInfo tosInfo = new TermsOfServiceInfo();

		String latestVersion = getCachedTermsOfServiceVersion(DEFAULT_VERSION);
		
		tosInfo.setLatestTermsOfServiceVersion(latestVersion);
		tosInfo.setTermsOfServiceUrl(String.format(TOS_URL_FORMAT, latestVersion));
		
		// TODO
		tosInfo.setCurrentRequirements(new TermsOfServiceRequirements()
			.setMinimumTermsOfServiceVersion(DEFAULT_VERSION)
			.setRequirementDate(DEFAULT_REQUIREMENT_DATE)
		);
		 
		return tosInfo;
	}
	
	String getCachedTermsOfServiceVersion(String version) {
		try {
			return versionCache.getUnchecked(VERSION_LATEST);
		} catch (UncheckedExecutionException e) {
			if (e.getCause() instanceof IllegalArgumentException) {
				throw (IllegalArgumentException) e.getCause();
			}
			if (e.getCause() instanceof NotFoundException) {
				throw (NotFoundException) e.getCause();
			}
			throw e;
		}
	}
	
	String fetchTermsOfServiceVersion(String version) {
		
		if (!VERSION_LATEST.equals(version)) {
			try {
				new SchemaIdParser(version).semanticVersion().toString();
			} catch (ParseException e) {
				throw new IllegalArgumentException(version + " is not a valid semantic version.");
			}
		}
		
		String url = String.format(TOS_API_VERSION, version);
		
		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(URI.create(url))
				.headers(GITHUB_REQUEST_HEADERS)
				.build();

		HttpResponse<String> response;
		
		try {
			response = httpClient.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Could not fetch {} TOS version: ", version, e);
			throw new RuntimeException(String.format(MESSAGE_VERSION_UNAVAILABLE, version), e);
		}
		
		HttpStatus status = HttpStatus.resolve(response.statusCode());
		
		if (!HttpStatus.OK.equals(status)) {
			LOGGER.error("Could not fetch {} TOS version (status code: {}): {}", version, response.statusCode(), response.body());
			
			String exMessage = String.format(MESSAGE_VERSION_UNAVAILABLE, version);
			
			if (HttpStatus.NOT_FOUND.equals(status)) {
				throw new NotFoundException(exMessage);
			}
			
			throw new RuntimeException(exMessage);
		}
		
		JSONObject responseBody;
		
		try {
			responseBody = new JSONObject(response.body());
		} catch (JSONException e) {
			LOGGER.error("Could not parse {} TOS version: {}", version, response.body(), e);
			throw new RuntimeException(String.format(MESSAGE_VERSION_UNAVAILABLE, version), e);
		}
		
		if (!responseBody.has(PROPERTY_RELEASE_TAG)) {
			LOGGER.error("Could not parse {} TOS version, missing {} property", version, PROPERTY_RELEASE_TAG, response.body());
			throw new RuntimeException(String.format(MESSAGE_VERSION_UNAVAILABLE, version));
		}
		
		return responseBody.getString(PROPERTY_RELEASE_TAG);
	}
		
}
