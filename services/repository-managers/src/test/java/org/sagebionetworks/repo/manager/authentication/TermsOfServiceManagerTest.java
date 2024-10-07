package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
import org.sagebionetworks.repo.model.utils.github.Release;
import org.sagebionetworks.repo.util.github.GithubApiClient;
import org.sagebionetworks.repo.util.github.GithubApiException;
import org.sagebionetworks.util.Clock;
import org.springframework.http.HttpStatus;

import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

@ExtendWith(MockitoExtension.class)
public class TermsOfServiceManagerTest {
	
	private static String INITIAL_LATEST_VERSION = "0.0.0";
	
	@Mock
	private AuthenticationDAO mockAuthDao;
	
	@Mock
	private GithubApiClient mockGithubClient;
	
	@Mock
	private Clock mockClock;
	
	@InjectMocks
	private TermsOfServiceManager manager;
	
	@Mock
	private TermsOfServiceRequirements mockRequirements;
	
	private Release release;
	
	@BeforeEach
	public void beforeEach() {
		release = new Release()
			.setId(123L)
			.setName("Latest Release")
			.setTag_name(INITIAL_LATEST_VERSION);
	}
	
	@Test
	public void testGetTermsOfUseInfo() {
		
		var managerSpy = Mockito.spy(manager);
		
		doReturn("2.0.0").when(managerSpy).getLatestVersion();
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(Optional.of(mockRequirements));
		
		TermsOfServiceInfo expected = new TermsOfServiceInfo()
			.setLatestTermsOfServiceVersion("2.0.0")
			.setTermsOfServiceUrl("https://raw.githubusercontent.com/Sage-Bionetworks/Sage-Governance-Documents/refs/tags/2.0.0/Terms.md")
			.setCurrentRequirements(mockRequirements);
		
		// Call under test
		assertEquals(expected, managerSpy.getTermsOfUseInfo());
	}
	
	@Test
	public void testGetTermsOfUseInfoWithNoData() {
		
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(Optional.empty());
		
		assertEquals("Terms of Service requirements not initialized.", assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			manager.getTermsOfUseInfo();
		}).getMessage());
	}
	
	@Test
	public void testInitialize() {
		
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(release);
				
		// Call under test
		manager.initialize();
		
		assertEquals(INITIAL_LATEST_VERSION, manager.getLatestVersionFallback());
		
	}
	
	@Test
	public void testInitializeWithException() {
		RuntimeException ex = new RuntimeException("failed");
		
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenThrow(ex);
		
		assertEquals(ex, assertThrows(UncheckedExecutionException.class, () -> {
			// Call under test
			manager.initialize();
		}).getCause());
		
		assertNull(manager.getLatestVersionFallback());
	}
	
	@Test
	public void testGetLatestVersion() {
		
		when(mockGithubClient.getLatestRelease(any(), any())).thenReturn(release);
		
		// Call under test
		assertEquals(INITIAL_LATEST_VERSION, manager.getLatestVersion());
		
		// Calling a second time fetches it from the cache
		assertEquals(INITIAL_LATEST_VERSION, manager.getLatestVersion());
		
		verify(mockGithubClient).getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO);
		
		expireVersionCache();
		
		release.setTag_name("3.0.0");
		
		// Now the call refreshes the cache
		assertEquals("3.0.0", manager.getLatestVersion());
		
		verify(mockGithubClient, times(2)).getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO);
	}

	static List<Throwable> getLatestVersionExceptions() {
		return List.of(
			new GithubApiException(new RuntimeException("failed")),
			new GithubApiException("failed", HttpStatus.BAD_GATEWAY, "requestBody"),
			new GithubApiException("failed", HttpStatus.BAD_GATEWAY, null),
			new UncheckedExecutionException(new RuntimeException("failed")),
			new ExecutionError(new Error("bad"))
		);
	}
	
	@ParameterizedTest
	@MethodSource("getLatestVersionExceptions")
	public void testGetLatestVersionWithExceptionAfterInitialize(Throwable ex) {
		when(mockGithubClient.getLatestRelease(any(), any())).thenReturn(release);
		
		manager.initialize();
		
		expireVersionCache();
		
		when(mockGithubClient.getLatestRelease(any(), any())).thenThrow(ex);
		
		// Call under test
		assertEquals(INITIAL_LATEST_VERSION, manager.getLatestVersion());
		
		verify(mockGithubClient, times(2)).getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO);
	}
		
	@ParameterizedTest
	@MethodSource("getLatestVersionExceptions")
	public void testGetLatestVersionWithExceptionAfterSuccess(Throwable ex) {
		when(mockGithubClient.getLatestRelease(any(), any())).thenReturn(release);
		
		assertEquals(INITIAL_LATEST_VERSION, manager.getLatestVersion());
		
		expireVersionCache();
		
		when(mockGithubClient.getLatestRelease(any(), any())).thenThrow(ex);
		
		// Call under test, get the latest recorded version
		assertEquals(INITIAL_LATEST_VERSION, manager.getLatestVersion());
		
		verify(mockGithubClient, times(2)).getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO);
	}
	
	@Test
	public void testFetchReleaseWithLatest() {
		when(mockGithubClient.getLatestRelease(any(), any())).thenReturn(release);
		
		// Call under test
		assertEquals(release, manager.fetchRelease("latest"));
		
		assertEquals(INITIAL_LATEST_VERSION, manager.getLatestVersionFallback());
		
		verify(mockGithubClient).getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO);
	}
	
	@Test
	public void testFetchReleaseWithTag() {
		when(mockGithubClient.getReleaseByTag(any(), any(), any())).thenReturn(release);
		
		// Call under test
		assertEquals(release, manager.fetchRelease("1.0.0"));
		
		assertNull(manager.getLatestVersionFallback());
		
		verify(mockGithubClient).getReleaseByTag(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO, "1.0.0");
	}
	
	@Test
	public void testFetchReleaseWithTagAndNonSemanticVersion() {		
		assertEquals("Expected a semantic version, was: 1.0", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.fetchRelease("1.0");
		}).getMessage());
		
		assertNull(manager.getLatestVersionFallback());
		
		verifyZeroInteractions(mockGithubClient);
	}

	private void expireVersionCache() {
		// By default the mock of nanoTime() returns 0, moving the clock forward to the expiration plus 1 second effectively invalidates the cache 
		when(mockClock.nanoTime()).thenReturn(TermsOfServiceManager.VERSION_CACHE_EXPIRATION.plusSeconds(1).toNanos());
	}
}
