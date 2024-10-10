package org.sagebionetworks.repo.manager.authentication;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.utils.github.Release;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.util.github.GithubApiClient;
import org.sagebionetworks.repo.util.github.GithubApiException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.semver4j.Semver;
import org.springframework.stereotype.Service;

@Service
public class TermsOfServiceManager {

	private static final Logger LOGGER = LogManager.getLogger(TermsOfServiceManager.class);

	static final String ORG = "Sage-Bionetworks";
	static final String REPO = "Sage-Governance-Documents";

	private static final String TOS_URL_FORMAT = "https://raw.githubusercontent.com/" + ORG + "/" + REPO + "/refs/tags/%s/Terms.md";

	private AuthenticationDAO authDao;

	private GithubApiClient githubClient;
	
	private Clock clock;

	public TermsOfServiceManager(AuthenticationDAO authDao, GithubApiClient githubClient, Clock clock) {
		this.authDao = authDao;
		this.githubClient = githubClient;
		this.clock = clock;
	}

	@PostConstruct
	public void initialize() {
		refreshLatestVersion();
	}

	/**
	 * @return The information about the current terms of service and requirements
	 */
	public TermsOfServiceInfo getTermsOfUseInfo() {
		TermsOfServiceInfo tosInfo = new TermsOfServiceInfo();

		tosInfo.setCurrentRequirements(authDao.getCurrentTermsOfServiceRequirements()
				.orElseThrow(() -> new IllegalStateException("Terms of Service requirements not initialized.")));

		String latestVersion = authDao.getTermsOfServiceLatestVersion()
				.orElseThrow(() -> new IllegalStateException("Terms of Service latest version not initialized."));

		tosInfo.setLatestTermsOfServiceVersion(latestVersion);
		tosInfo.setTermsOfServiceUrl(String.format(TOS_URL_FORMAT, latestVersion));

		return tosInfo;
	}

	@WriteTransaction
	public void signTermsOfService(Long principalId, String termsOfServiceVersion) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean hasUserAcceptedTermsOfService(long userId) {
		// TODO
		return true;
	}

	public void refreshLatestVersion() {
		
		LOGGER.info("Fetching latest ToS version from github...");

		Release latestRelease;

		try {
			latestRelease = githubClient.getLatestRelease(ORG, REPO);
		} catch (GithubApiException e) {
			LOGGER.error("Could not fetch latest ToS version: ", e);
			if (e.getResponseBody() != null || e.getStatus() != null) {
				LOGGER.error("Response from github was: {} (status: {})", e.getResponseBody(), e.getStatus());
			}
			throw e;
		}
		
		Semver version = validateSemver(new Semver(latestRelease.getTag_name()));

		authDao.setTermsOfServiceLatestVersion(version.getVersion());

		LOGGER.info("Fetching latest ToS version from github...DONE (Name: {}, Tag: {})", latestRelease.getName(),
				latestRelease.getTag_name());
	}
	
	private static Semver validateSemver(Semver version) {
		ValidateArgument.requirement(version.getPreRelease() == null || version.getPreRelease().isEmpty(), "Unsupported version format: prerelease should not be included.");
		ValidateArgument.requirement(version.getBuild() == null || version.getBuild().isEmpty(), "Unsupported version format: build metadata should not be included.");
		return version;
	}

}
