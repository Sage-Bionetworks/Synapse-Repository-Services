package org.sagebionetworks.repo.manager.authentication;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
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

	private static final String VERSION_LATEST = "latest";
	
	static final Duration VERSION_CACHE_EXPIRATION = Duration.ofHours(24);
	
	static final String ORG = "Sage-Bionetworks";
	static final String REPO = "Sage-Governance-Documents";
	
	private static final String TOS_URL_FORMAT = "https://raw.githubusercontent.com/" + ORG + "/" + REPO + "/refs/tags/%s/Terms.md";
		
	private AuthenticationDAO authDao;
	
	private GithubApiClient githubClient;
	
	private LoadingCache<String, Release> versionCache;
	
	private volatile String latestVersionFallback;
	
	public TermsOfServiceManager(AuthenticationDAO authDao, GithubApiClient githubClient, Clock clock) {
		this.authDao = authDao;
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
		// We pre-load the latest version letting the exceptions go through so the server does not start if this fails
		versionCache.getUnchecked(VERSION_LATEST);
	}

	/**
	 * @return The information about the current terms of service and requirements
	 */
	public TermsOfServiceInfo getTermsOfUseInfo() {
		TermsOfServiceInfo tosInfo = new TermsOfServiceInfo();
		
		tosInfo.setCurrentRequirements(authDao.getCurrentTermsOfServiceRequirements()
				.orElseThrow(() -> new IllegalStateException("Terms of Service requirements not initialized.")));

		String latestVersion = getLatestVersion();
		
		tosInfo.setLatestTermsOfServiceVersion(latestVersion);
		tosInfo.setTermsOfServiceUrl(String.format(TOS_URL_FORMAT, latestVersion));
		 
		return tosInfo;
	}
	
	String getLatestVersion() {
		String latestVersion;
		
		try {
			latestVersion = versionCache.getUnchecked(VERSION_LATEST).getTag_name();
		} catch (UncheckedExecutionException | ExecutionError e ) {
			// We do not want an issue with github to bring down synapse, fallback on the latest known version
			LOGGER.error("Could not fetch latest version, will fallback to version {}: ", latestVersionFallback, e);
			if (e.getCause() instanceof GithubApiException) {
				GithubApiException githubApiEx = (GithubApiException) e.getCause();
				if (githubApiEx.getResponseBody() != null || githubApiEx.getStatus() != null) {
					LOGGER.error("Response from github was: {} (status: {})", githubApiEx.getResponseBody(), githubApiEx.getStatus());	
				}
			}
			// We fallback on the known latest fallback version
			latestVersion = latestVersionFallback;
		}
		
		return latestVersion;
	}

	Release fetchRelease(String tag) {
		LOGGER.info("Fetching github release for {} version...", tag);
		
		Release release;
		
		if (VERSION_LATEST.equals(tag)) {
			release = githubClient.getLatestRelease(ORG, REPO);
			// Makes sure to update the latest version fallback each time we fetch a new value
			latestVersionFallback = release.getTag_name();
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
	
	String getLatestVersionFallback() {
		return latestVersionFallback;
	}
}
