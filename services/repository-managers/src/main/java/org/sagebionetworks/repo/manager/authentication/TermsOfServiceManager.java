package org.sagebionetworks.repo.manager.authentication;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
import org.sagebionetworks.repo.model.utils.github.Release;
import org.sagebionetworks.repo.util.github.GithubApiClient;
import org.sagebionetworks.repo.util.github.GithubApiException;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.util.Clock;
import org.springframework.stereotype.Service;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

@Service
public class TermsOfServiceManager {
	
	private static final Logger LOGGER = LogManager.getLogger(TermsOfServiceManager.class);	
	
	private static final String DEFAULT_VERSION = "0.0.0";
	private static final Date DEFAULT_REQUIREMENT_DATE = Date.from(Instant.parse("2011-01-01T00:00:00.000Z"));

	private static final String VERSION_LATEST = "latest";
	
	private static final Duration VERSION_CACHE_EXPIRATION = Duration.of(10, ChronoUnit.MINUTES);
	
	static final String ORG = "Sage-Bionetworks";
	static final String REPO = "Sage-Governance-Documents";
	
	private static final String TOS_URL_FORMAT = "https://raw.githubusercontent.com/" + ORG + "/" + REPO + "/refs/tags/%s/Terms.md";
		
	private GithubApiClient githubClient;
	
	private LoadingCache<String, Release> versionCache;
	
	private String latestVersionFallback = DEFAULT_VERSION;
	
	public TermsOfServiceManager(GithubApiClient githubClient, Clock clock) {
		this.githubClient = githubClient;
		this.versionCache = CacheBuilder.newBuilder()
			.ticker(new Ticker() {
				
				@Override
				public long read() {
					return clock.nanoTime();
				}
			})
			.expireAfterWrite(VERSION_CACHE_EXPIRATION)
			.build(CacheLoader.from(this::fetchRelease));
	}
	
	@PostConstruct
	public void initialize() {
		// We load some version when we start so there is always a known latest version
		latestVersionFallback = getLatestVersion();
	}

	/**
	 * @return The information about the current terms of service and requirements
	 */
	public TermsOfServiceInfo getTermsOfUseInfo() {
		TermsOfServiceInfo tosInfo = new TermsOfServiceInfo();

		String latestVersion = getLatestVersion();
		
		tosInfo.setLatestTermsOfServiceVersion(latestVersion);
		tosInfo.setTermsOfServiceUrl(String.format(TOS_URL_FORMAT, latestVersion));
		
		// TODO, get this from the database?
		tosInfo.setCurrentRequirements(new TermsOfServiceRequirements()
			.setMinimumTermsOfServiceVersion(DEFAULT_VERSION)
			.setRequirementDate(DEFAULT_REQUIREMENT_DATE)
		);
		 
		return tosInfo;
	}
	
	String getLatestVersion() {
		String latestVersion;
		
		try {
			latestVersion = versionCache.getUnchecked(VERSION_LATEST).getTag_name();
			latestVersionFallback = latestVersion;
		} catch (UncheckedExecutionException | ExecutionError e ) {
			// We do not want an issue with github to bring down synapse, fallback on the latest known version
			LOGGER.error("Could not fetch latest version, will fallback to version {}: ", latestVersionFallback, e);
			if (e.getCause() instanceof GithubApiException) {
				GithubApiException githubApiEx = (GithubApiException) e.getCause();
				if (githubApiEx.getResponseBody() != null || githubApiEx.getStatus() != null) {
					LOGGER.error("Reponse from github was: {} (status: {})", githubApiEx.getResponseBody(), githubApiEx.getStatus());	
				}
			}
			latestVersion = latestVersionFallback;
		}		
		
		return latestVersion;
	}

	Release fetchRelease(String tag) {
		LOGGER.info("Fetching github release for {} version...", tag);
		
		Release release;
		
		if (VERSION_LATEST.equals(tag)) {
			release = githubClient.getLatestRelease(ORG, REPO);
		} else {
			try {
				new SchemaIdParser(tag).semanticVersion();
			} catch (ParseException e) {
				throw new IllegalArgumentException("Expected a semantic version, was: " + tag);
			}
			
			release = githubClient.getReleaseByTag(ORG, REPO, tag);
		}
		
		LOGGER.info("Fetching github release for {} version...DONE (Name: {}, Tag: {})", tag, release.getName(), release.getTag_name());
		
		return release;
	}
	
		
}
