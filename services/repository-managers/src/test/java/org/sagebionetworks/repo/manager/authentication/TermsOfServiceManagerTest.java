package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
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
	
	@BeforeEach
	public void beforeEach() {
		
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
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(Optional.of(mockRequirements));
		when(mockAuthDao.getTermsOfServiceLatestVersion()).thenReturn(Optional.of("2.0.0"));
		
		TermsOfServiceInfo expected = new TermsOfServiceInfo()
			.setLatestTermsOfServiceVersion("2.0.0")
			.setTermsOfServiceUrl("https://raw.githubusercontent.com/Sage-Bionetworks/Sage-Governance-Documents/refs/tags/2.0.0/Terms.md")
			.setCurrentRequirements(mockRequirements);
		
		// Call under test
		assertEquals(expected, manager.getTermsOfUseInfo());
	}
	
	@Test
	public void testGetTermsOfUseInfoWithMissingRequirements() {
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(Optional.empty());
		
		assertEquals("Terms of Service requirements not initialized.", assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			manager.getTermsOfUseInfo();
		}).getMessage());
	}
	
	@Test
	public void testGetTermsOfUseInfoWithMissingLatestVersion() {
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(Optional.of(mockRequirements));
		when(mockAuthDao.getTermsOfServiceLatestVersion()).thenReturn(Optional.empty());
		
		assertEquals("Terms of Service latest version not initialized.", assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			manager.getTermsOfUseInfo();
		}).getMessage());
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
	public void testRefreshLatestVersionWithPreRelease() {
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(new Release()
			.setName("Latest")
			.setTag_name("1.0.0-beta")
		);
		
		assertEquals("Unsupported version format: prerelease should not be included.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.refreshLatestVersion();
		}).getMessage());
		
		verifyZeroInteractions(mockAuthDao);
	}
	
	@Test
	public void testRefreshLatestVersionWithBuildMetadata() {
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(new Release()
			.setName("Latest")
			.setTag_name("1.0.0+abcd")
		);
		
		assertEquals("Unsupported version format: build metadata should not be included.", assertThrows(IllegalArgumentException.class, () -> {			
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
