package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
	
	@Mock
	private AuthenticationDAO mockAuthDao;
	
	@Mock
	private GithubApiClient mockGithubClient;
	
	@Mock
	private Clock mockClock;
	
	@InjectMocks
	private TermsOfServiceManager manager;
	
	@Mock
	private Release mockRelease;
	
	@Mock
	private TermsOfServiceRequirements mockRequirements;
	
	@Test
	public void testGetTermsOfUseInfo() {
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(Optional.of(mockRequirements));
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(mockRelease);
		when(mockRelease.getTag_name()).thenReturn("2.0.0");
		
		TermsOfServiceInfo expected = new TermsOfServiceInfo()
			.setLatestTermsOfServiceVersion("2.0.0")
			.setTermsOfServiceUrl("https://raw.githubusercontent.com/Sage-Bionetworks/Sage-Governance-Documents/refs/tags/2.0.0/Terms.md")
			.setCurrentRequirements(mockRequirements);
		
		// Call under test
		assertEquals(expected, manager.getTermsOfUseInfo());
	}
	
	@Test
	public void testGetTermsOfUseInfoWithNoData() {
		when(mockAuthDao.getCurrentTermsOfServiceRequirements()).thenReturn(Optional.empty());
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(mockRelease);
		when(mockRelease.getTag_name()).thenReturn("2.0.0");
		
		TermsOfServiceInfo expected = new TermsOfServiceInfo()
			.setLatestTermsOfServiceVersion("2.0.0")
			.setTermsOfServiceUrl("https://raw.githubusercontent.com/Sage-Bionetworks/Sage-Governance-Documents/refs/tags/2.0.0/Terms.md")
			.setCurrentRequirements(new TermsOfServiceRequirements()
				.setMinimumTermsOfServiceVersion("0.0.0")
				.setRequirementDate(Date.from(Instant.parse("2011-01-01T00:00:00.000Z")))
			);
		
		// Call under test
		assertEquals(expected, manager.getTermsOfUseInfo());
	}
	
	@Test
	public void testInitialize() {
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(mockRelease);
		when(mockRelease.getTag_name()).thenReturn("2.0.0");
		
		// Call under test
		manager.initialize();
	}
	
	@Test
	public void testInitializeWithException() {
		RuntimeException ex = new RuntimeException("failed");
		
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenThrow(ex);
		
		assertEquals(ex, assertThrows(UncheckedExecutionException.class, () -> {
			// Call under test
			manager.initialize();
		}).getCause());
	}
	
	@Test
	public void testGetLatestVersion() {
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(mockRelease);
		when(mockRelease.getTag_name()).thenReturn("2.0.0");
		
		// Call under test
		assertEquals("2.0.0", manager.getLatestVersion());
		
		// Calling a second time fetches it from the cache
		assertEquals("2.0.0", manager.getLatestVersion());
		
		verify(mockGithubClient, atMost(1)).getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO);
		
		// Clear the cache content by moving the time ahead
		when(mockClock.nanoTime()).thenReturn(System.nanoTime());
		
		// Now the call refreshes the cache
		assertEquals("2.0.0", manager.getLatestVersion());
		
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
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(mockRelease);
		when(mockRelease.getTag_name()).thenReturn("1.0.0");
		
		// This is invoked by spring after construction
		manager.initialize();
		
		// Clear the cache
		when(mockClock.nanoTime()).thenReturn(System.nanoTime());
		
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenThrow(ex);
		
		// Call under test
		assertEquals("1.0.0", manager.getLatestVersion());
		
		verify(mockGithubClient, times(2)).getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO);
	}
		
	@ParameterizedTest
	@MethodSource("getLatestVersionExceptions")
	public void testGetLatestVersionWithExceptionAfterSuccess(Throwable ex) {
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(mockRelease);
		when(mockRelease.getTag_name()).thenReturn("2.0.0");
		
		assertEquals("2.0.0", manager.getLatestVersion());
		
		// Clear the cache
		when(mockClock.nanoTime()).thenReturn(System.nanoTime());
		
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenThrow(ex);
		
		// Call under test, get the "default" version
		assertEquals("2.0.0", manager.getLatestVersion());
	}
	
	@Test
	public void testFetchReleaseWithLatest() {
		when(mockGithubClient.getLatestRelease(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO)).thenReturn(mockRelease);
		
		// Call under test
		assertEquals(mockRelease, manager.fetchRelease("latest"));
	}
	
	@Test
	public void testFetchReleaseWithTag() {
		when(mockGithubClient.getReleaseByTag(TermsOfServiceManager.ORG, TermsOfServiceManager.REPO, "1.0.0")).thenReturn(mockRelease);
		
		// Call under test
		assertEquals(mockRelease, manager.fetchRelease("1.0.0"));
	}
	
	@Test
	public void testFetchReleaseWithNonSemanticVersion() {
		
		assertEquals("Expected a semantic version, was: 1.0", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.fetchRelease("1.0");
		}).getMessage());
	}

}
