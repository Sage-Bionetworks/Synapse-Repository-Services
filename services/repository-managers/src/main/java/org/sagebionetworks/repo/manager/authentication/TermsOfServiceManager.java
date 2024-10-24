package org.sagebionetworks.repo.manager.authentication;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
import org.sagebionetworks.repo.model.auth.TermsOfServiceState;
import org.sagebionetworks.repo.model.auth.TermsOfServiceStatus;
import org.sagebionetworks.repo.model.utils.github.Release;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.util.github.GithubApiClient;
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
	public TermsOfServiceInfo getTermsOfServiceInfo() {
		TermsOfServiceInfo tosInfo = new TermsOfServiceInfo();

		tosInfo.setCurrentRequirements(authDao.getCurrentTermsOfServiceRequirements());

		String latestVersion = authDao.getTermsOfServiceLatestVersion();

		tosInfo.setLatestTermsOfServiceVersion(latestVersion);
		tosInfo.setTermsOfServiceUrl(String.format(TOS_URL_FORMAT, latestVersion));

		return tosInfo;
	}

	@WriteTransaction
	public void signTermsOfService(long principalId, String termsOfServiceVersion) {
		ValidateArgument.requirement(!BOOTSTRAP_PRINCIPAL.isBootstrapPrincipalId(principalId), "The given user cannot sign the terms of service.");
		
		Semver versionToSign = parseSemver(termsOfServiceVersion == null ? AuthenticationDAO.DEFAULT_TOS_REQUIREMENTS.getMinimumTermsOfServiceVersion() : termsOfServiceVersion);
		Semver latestVersion = new Semver(authDao.getTermsOfServiceLatestVersion());
		
		ValidateArgument.requirement(versionToSign.isLowerThanOrEqualTo(latestVersion),
				String.format("The version cannot be greater than the latest available version (%s).", latestVersion.getVersion()));
		
		TermsOfServiceRequirements requirements = authDao.getCurrentTermsOfServiceRequirements();		
		
		ValidateArgument.requirement(versionToSign.isGreaterThanOrEqualTo(requirements.getMinimumTermsOfServiceVersion()),
					String.format("The version cannot be lower than the current required version (%s).", requirements.getMinimumTermsOfServiceVersion()));
		
		authDao.addTermsOfServiceAgreement(principalId, versionToSign.getVersion(), clock.now());
	}
	
	public TermsOfServiceStatus getUserTermsOfServiceStatus(long userId) {
		ValidateArgument.requirement(!AuthorizationUtils.isUserAnonymous(userId), "Cannot get terms of service status for the anonymous user.");
		
		TermsOfServiceStatus status = new TermsOfServiceStatus()
			.setUserId(String.valueOf(userId))
			.setUserCurrentTermsOfServiceState(TermsOfServiceState.MUST_AGREE_NOW);
		
		// Any user that is bootstrapped is always considered up-to-date (e.g. admin etc)
		if (BOOTSTRAP_PRINCIPAL.isBootstrapPrincipalId(userId)) {
			status.setUserCurrentTermsOfServiceState(TermsOfServiceState.UP_TO_DATE);
		} else {
			authDao.getLatestTermsOfServiceAgreement(userId).ifPresent( existing -> {
				status.setLastAgreementDate(existing.getAgreedOn());
				status.setLastAgreementVersion(existing.getVersion());
				
				TermsOfServiceRequirements requirements = authDao.getCurrentTermsOfServiceRequirements();
				
				if (new Semver(existing.getVersion()).isGreaterThanOrEqualTo(requirements.getMinimumTermsOfServiceVersion())) {
					status.setUserCurrentTermsOfServiceState(TermsOfServiceState.UP_TO_DATE);
				} else if (clock.now().after(requirements.getRequirementDate())) {
					status.setUserCurrentTermsOfServiceState(TermsOfServiceState.MUST_AGREE_NOW);
				} else {
					status.setUserCurrentTermsOfServiceState(TermsOfServiceState.MUST_AGREE_SOON);
				}
				
			});
		}
		
		return status;
	}
	
	@WriteTransaction
	public TermsOfServiceInfo updateTermsOfServiceRequirements(UserInfo userInfo, TermsOfServiceRequirements requirements) {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(requirements, "The requirements");
		ValidateArgument.required(requirements.getRequirementDate(), "The requirement date");
		ValidateArgument.required(requirements.getMinimumTermsOfServiceVersion(), "The minimum version");
		
		if (!AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only an ACT member or an administrator can perform this operation.");
		}
		
		Semver minVersion = parseSemver(requirements.getMinimumTermsOfServiceVersion());
		Semver latestVersion = refreshLatestVersion();
		
		ValidateArgument.requirement(minVersion.isLowerThanOrEqualTo(latestVersion), "The minium version cannot be greater than the latest available version.");
		
		authDao.setCurrentTermsOfServiceRequirements(userInfo.getId(), minVersion.getVersion(), requirements.getRequirementDate());
		
		return getTermsOfServiceInfo();
	}
	
	public boolean hasUserAcceptedTermsOfService(long userId) {
		return !TermsOfServiceState.MUST_AGREE_NOW.equals(getUserTermsOfServiceStatus(userId).getUserCurrentTermsOfServiceState());
	}

	public Semver refreshLatestVersion() {
		
		LOGGER.info("Fetching latest ToS version from github...");

		Release latestRelease = githubClient.getLatestRelease(ORG, REPO);
		
		Semver version = parseSemver(latestRelease.getTag_name());

		authDao.setTermsOfServiceLatestVersion(version.getVersion());

		LOGGER.info("Fetching latest ToS version from github...DONE (Name: {}, Tag: {})", latestRelease.getName(),
				latestRelease.getTag_name());
		
		return version;
	}
	
	private static Semver parseSemver(String version) {
		ValidateArgument.requiredNotBlank(version, "The version");
		Semver parsedVersion = new Semver(version);
		ValidateArgument.requirement(parsedVersion.getPreRelease().isEmpty() && parsedVersion.getBuild().isEmpty(), "Unsupported version format: should not include pre-release or build metadata.");
		return parsedVersion;
	}

}
