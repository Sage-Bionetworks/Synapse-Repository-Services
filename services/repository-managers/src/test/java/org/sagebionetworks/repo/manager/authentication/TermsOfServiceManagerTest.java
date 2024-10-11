package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceAgreement;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
import org.sagebionetworks.repo.model.auth.TermsOfServiceState;
import org.sagebionetworks.repo.model.auth.TermsOfServiceStatus;
import org.sagebionetworks.repo.model.utils.github.Release;
import org.sagebionetworks.repo.util.github.GithubApiClient;
import org.sagebionetworks.repo.util.github.GithubApiException;
import org.sagebionetworks.util.Clock;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class TermsOfServiceManagerTest {
	
	@Mock
	private AuthenticationDAO mockAuthDao;
	
	@Mock
	private GithubApiClient mockGithubClient;
	
	@Mock
	private CountingSemaphore mockCountingSemaphore;
	
	@Mock
	private Clock mockClock;
	
	@InjectMocks
	private TermsOfServiceManager manager;
	
	@Mock
	private TermsOfServiceRequirements mockRequirements;
	
	private long userId;
	
	@BeforeEach
	public void beforeEach() {
		userId = 123;
	}
	
	@Test
	public void testInitialize() {
		TermsOfServiceManager managerSpy = Mockito.spy(manager);
		
		doNothing().when(managerSpy).refreshLatestVersion();
		
		// Call under test
		managerSpy.initialize();
	}
	
	@Test
	public void testGetTermsOfUseInfo() {
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(mockRequirements);
		when(mockAuthDao.getTermsOfServiceLatestVersion()).thenReturn("2.0.0");
		
		TermsOfServiceInfo expected = new TermsOfServiceInfo()
			.setLatestTermsOfServiceVersion("2.0.0")
			.setTermsOfServiceUrl("https://raw.githubusercontent.com/Sage-Bionetworks/Sage-Governance-Documents/refs/tags/2.0.0/Terms.md")
			.setCurrentRequirements(mockRequirements);
		
		// Call under test
		assertEquals(expected, manager.getTermsOfUseInfo());
	}
	
	@Test
	public void signTermsOfService() {
		
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);
		when(mockAuthDao.getTermsOfServiceLatestVersion()).thenReturn("1.0.0");
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(new TermsOfServiceRequirements()
			.setMinimumTermsOfServiceVersion("1.0.0")
		);
		
		// Call under test
		manager.signTermsOfService(userId, "1.0.0");
		
		verify(mockAuthDao).addTermsOfServiceAgreement(userId, "1.0.0", now);
	}
	
	@Test
	public void signTermsOfServiceWithNoVersion() {
		
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);
		when(mockAuthDao.getTermsOfServiceLatestVersion()).thenReturn("1.0.0");
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(
			AuthenticationDAO.DEFAULT_TOS_REQUIREMENTS
		);
		
		// Call under test
		manager.signTermsOfService(userId, null);
		
		verify(mockAuthDao).addTermsOfServiceAgreement(userId, AuthenticationDAO.DEFAULT_TOS_REQUIREMENTS.getMinimumTermsOfServiceVersion(), now);
	}
	
	@Test
	public void signTermsOfServiceWithVersionLowerThanRequired() {
		
		when(mockAuthDao.getTermsOfServiceLatestVersion()).thenReturn("2.0.0");
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(new TermsOfServiceRequirements()
			.setMinimumTermsOfServiceVersion("1.0.0")
		);
		
		assertEquals("The version cannot be lower than the current required version (1.0.0).", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.signTermsOfService(userId, "0.0.0");
		}).getMessage());

		verifyNoMoreInteractions(mockAuthDao);
	}

	@Test
	public void signTermsOfServiceWithVersionGreaterThanLatest() {
		
		when(mockAuthDao.getTermsOfServiceLatestVersion()).thenReturn("1.0.0");
			
		assertEquals("The version cannot be greater than the latest available version (1.0.0).", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.signTermsOfService(userId, "2.0.0");
		}).getMessage());

		verifyNoMoreInteractions(mockAuthDao);
	}
		
	@Test
	public void signTermsOfServiceWithInvalidVersion() {
				
		assertEquals("Unsupported version format.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.signTermsOfService(userId, "1.0.0-alpha");
		}).getMessage());
		
		assertEquals("Unsupported version format.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.signTermsOfService(userId, "1.0.0+abcd");
		}).getMessage());
		
		verifyNoMoreInteractions(mockAuthDao);
	}
	
	@ParameterizedTest
	@EnumSource(BOOTSTRAP_PRINCIPAL.class)
	public void signTermsOfServiceWithBootstrapUser(BOOTSTRAP_PRINCIPAL principal) {
		userId = principal.getPrincipalId();
		
		assertEquals("The given user cannot sign the terms of service.", assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.signTermsOfService(userId, "2.0.0");
		}).getMessage());

		verifyNoMoreInteractions(mockAuthDao);
	}
	
	@Test
	public void testGetUserTermsOfServiceStatus() {
		Date agreementDate = new Date();
		
		when(mockAuthDao.getLatestTermsOfServiceAgreement(userId)).thenReturn(Optional.of(new TermsOfServiceAgreement()
			.setAgreedOn(agreementDate)
			.setVersion("1.0.0")
		));
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(new TermsOfServiceRequirements()
			.setRequirementDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
			.setMinimumTermsOfServiceVersion("1.0.0")
		);		
		
		TermsOfServiceStatus expected = new TermsOfServiceStatus()
			.setUserId(String.valueOf(userId))
			.setLastAgreementDate(agreementDate)
			.setLastAgreementVersion("1.0.0")
			.setUserCurrentTermsOfServiceState(TermsOfServiceState.UP_TO_DATE);
		
		// Call under test
		assertEquals(expected, manager.getUserTermsOfServiceStatus(userId));
		verifyNoMoreInteractions(mockAuthDao);
	}
	
	@Test
	public void testGetUserTermsOfServiceStatusWithNoAgreement() {
		when(mockAuthDao.getLatestTermsOfServiceAgreement(userId)).thenReturn(Optional.empty());
				
		TermsOfServiceStatus expected = new TermsOfServiceStatus()
			.setUserId(String.valueOf(userId))
			.setLastAgreementDate(null)
			.setLastAgreementVersion(null)
			.setUserCurrentTermsOfServiceState(TermsOfServiceState.MUST_AGREE_NOW);
		
		// Call under test
		assertEquals(expected, manager.getUserTermsOfServiceStatus(userId));
		verifyNoMoreInteractions(mockAuthDao);
	}
	
	@Test
	public void testGetUserTermsOfServiceStatusWithVersionHigherThanRequirements() {
		Date agreementDate = new Date();
		
		when(mockAuthDao.getLatestTermsOfServiceAgreement(userId)).thenReturn(Optional.of(new TermsOfServiceAgreement()
			.setAgreedOn(agreementDate)
			.setVersion("1.0.0")
		));
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(new TermsOfServiceRequirements()
			.setMinimumTermsOfServiceVersion("0.0.0")
		);		
		
		TermsOfServiceStatus expected = new TermsOfServiceStatus()
			.setUserId(String.valueOf(userId))
			.setLastAgreementDate(agreementDate)
			.setLastAgreementVersion("1.0.0")
			.setUserCurrentTermsOfServiceState(TermsOfServiceState.UP_TO_DATE);
		
		// Call under test
		assertEquals(expected, manager.getUserTermsOfServiceStatus(userId));
		verifyNoMoreInteractions(mockAuthDao);
	}
	
	@Test
	public void testGetUserTermsOfServiceStatusWithUpcomingRequirements() {
		when(mockClock.now()).thenReturn(new Date());
		
		Date agreementDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
		
		when(mockAuthDao.getLatestTermsOfServiceAgreement(userId)).thenReturn(Optional.of(new TermsOfServiceAgreement()
			.setAgreedOn(agreementDate)
			.setVersion("1.0.0")
		));
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(new TermsOfServiceRequirements()
			.setRequirementDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
			.setMinimumTermsOfServiceVersion("2.0.0")
		);		
		
		TermsOfServiceStatus expected = new TermsOfServiceStatus()
			.setUserId(String.valueOf(userId))
			.setLastAgreementDate(agreementDate)
			.setLastAgreementVersion("1.0.0")
			.setUserCurrentTermsOfServiceState(TermsOfServiceState.MUST_AGREE_SOON);
		
		// Call under test
		assertEquals(expected, manager.getUserTermsOfServiceStatus(userId));
		verifyNoMoreInteractions(mockAuthDao);
	}
	
	@Test
	public void testGetUserTermsOfServiceStatusWithOutdatedVersion() {
		when(mockClock.now()).thenReturn(new Date());
		
		Date agreementDate = Date.from(Instant.now().minus(30, ChronoUnit.DAYS));
		
		when(mockAuthDao.getLatestTermsOfServiceAgreement(userId)).thenReturn(Optional.of(new TermsOfServiceAgreement()
			.setAgreedOn(agreementDate)
			.setVersion("1.0.0")
		));
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(new TermsOfServiceRequirements()
			.setRequirementDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
			.setMinimumTermsOfServiceVersion("2.0.0")
		);		
		
		TermsOfServiceStatus expected = new TermsOfServiceStatus()
			.setUserId(String.valueOf(userId))
			.setLastAgreementDate(agreementDate)
			.setLastAgreementVersion("1.0.0")
			.setUserCurrentTermsOfServiceState(TermsOfServiceState.MUST_AGREE_NOW);
		
		// Call under test
		assertEquals(expected, manager.getUserTermsOfServiceStatus(userId));
		verifyNoMoreInteractions(mockAuthDao);
	}
	
	@Test
	public void testGetUserTermsOfServiceStatusWithAnonymousUser() {
		userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		
		assertEquals("Cannot get terms of service status for the anonymous user.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getUserTermsOfServiceStatus(userId);
		}).getMessage());
		
		verifyNoMoreInteractions(mockAuthDao);
	}
	
	@ParameterizedTest
	@EnumSource(mode = Mode.EXCLUDE, value = BOOTSTRAP_PRINCIPAL.class, names = {"ANONYMOUS_USER"})
	public void testGetUserTermsOfServiceStatusWithBootstrapPrincipals(BOOTSTRAP_PRINCIPAL principal) {
		userId = principal.getPrincipalId();
		
		TermsOfServiceStatus expected = new TermsOfServiceStatus()
				.setUserId(String.valueOf(userId))
				.setLastAgreementDate(null)
				.setLastAgreementVersion(null)
				.setUserCurrentTermsOfServiceState(TermsOfServiceState.UP_TO_DATE);

		// Call under test
		assertEquals(expected, manager.getUserTermsOfServiceStatus(userId));
		
		verifyNoMoreInteractions(mockAuthDao);
	}
	
	@ParameterizedTest
	@EnumSource(value = TermsOfServiceState.class)
	public void testHasUserAcceptedTermsOfService(TermsOfServiceState userTosState) {
		TermsOfServiceManager managerSpy = Mockito.spy(manager);
		
		doReturn(new TermsOfServiceStatus()
			.setUserCurrentTermsOfServiceState(userTosState)
		).when(managerSpy).getUserTermsOfServiceStatus(userId);
		
		// Call under test
		assertEquals(!TermsOfServiceState.MUST_AGREE_NOW.equals(userTosState), managerSpy.hasUserAcceptedTermsOfService(userId));
	}
		
	@Test
	public void testRefreshLatestVersion() {
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(new Release()
			.setName("Latest")
			.setTag_name("1.0.0")
		);
		
		// Call under test
		manager.refreshLatestVersion();
		
		verify(mockAuthDao).setTermsOfServiceLatestVersion("1.0.0");
	}
	
	@Test
	public void testRefreshLatestVersionWithUnsupportedFormat() {
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(
			new Release()
				.setName("Latest")
				.setTag_name("1.0.0-beta"),
			new Release()
				.setName("Latest")
				.setTag_name("1.0.0+abcd")
		);
		
		assertEquals("Unsupported version format.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.refreshLatestVersion();
		}).getMessage());
		
		assertEquals("Unsupported version format.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.refreshLatestVersion();
		}).getMessage());
		
		verifyZeroInteractions(mockAuthDao);
	}
	
	@Test
	public void testRefreshLatestVersionWithGithubApiException() {
		
		GithubApiException ex = new GithubApiException("failed", HttpStatus.BAD_GATEWAY, "bad");
		
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenThrow(ex);
		
		assertEquals(ex, assertThrows(GithubApiException.class, () -> {
			// Call under test
			manager.refreshLatestVersion();
		}));
		
		verifyZeroInteractions(mockAuthDao);
	}
	
}
