package org.sagebionetworks.repo.util.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.utils.github.Release;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class GithubApiClientImplTest {
	
	@Mock
	private HttpClient mockHttpClient;
	
	@InjectMocks
	@Spy
	private GithubApiClientImpl client;
	
	@Mock
	private HttpResponse<String> mockResponse;
	
	@Mock
	private CompletableFuture<HttpResponse<String>> mockResponseFuture;
	
	@Mock
	private CompletableFuture<Release> mockReleaseFuture;
	
	private String org = "SageBionetworks";
	private String repo = "repo";

	@Test
	public void testGetLatestRelease() {
		Release expected = new Release().setId(123L);
		
		doReturn(expected).when(client).getRelease(org, repo, "latest");
		
		// Call under test
		assertEquals(expected, client.getLatestRelease(org, repo));
		
	}
	
	@Test
	public void testGetReleaseByTag() {
		Release expected = new Release().setId(123L);
		
		doReturn(expected).when(client).getRelease(org, repo, "tags/1.0.0");
		
		// Call under test
		assertEquals(expected, client.getReleaseByTag(org, repo, "1.0.0"));
		
	}
	
	@Test
	public void testGetRelease() throws InterruptedException, ExecutionException {

		Release expected = new Release().setId(123L);
		
		when(mockHttpClient.sendAsync(expectedReleaseRequest("someTag"), GithubApiClientImpl.BODY_HANDLER)).thenReturn(mockResponseFuture);		
		when(mockResponseFuture.handle(GithubApiClientImpl.RESPONSE_HANDLER)).thenReturn(mockResponseFuture);
		when(mockResponseFuture.thenApply(GithubApiClientImpl.RELEASE_MAPPER)).thenReturn(mockReleaseFuture);
		when(mockReleaseFuture.get()).thenReturn(expected);
		
		// Call under test
		assertEquals(expected, client.getRelease(org, repo, "someTag"));
	}
	
	@Test
	public void testGetReleaseWithInterruptedException() throws InterruptedException, ExecutionException {

		InterruptedException ex = new InterruptedException("failed");
		
		when(mockHttpClient.sendAsync(expectedReleaseRequest("someTag"), GithubApiClientImpl.BODY_HANDLER)).thenReturn(mockResponseFuture);		
		when(mockResponseFuture.handle(GithubApiClientImpl.RESPONSE_HANDLER)).thenReturn(mockResponseFuture);
		when(mockResponseFuture.thenApply(GithubApiClientImpl.RELEASE_MAPPER)).thenReturn(mockReleaseFuture);
		when(mockReleaseFuture.get()).thenThrow(ex);		
		
		assertEquals(ex, assertThrows(GithubApiException.class, () -> {			
			// Call under test
			client.getRelease(org, repo, "someTag");
		}).getCause());
	}
	
	@Test
	public void testGetReleaseWithExecutionException() throws InterruptedException, ExecutionException {

		ExecutionException ex = new ExecutionException("failed", new RuntimeException());
		
		when(mockHttpClient.sendAsync(expectedReleaseRequest("someTag"), GithubApiClientImpl.BODY_HANDLER)).thenReturn(mockResponseFuture);		
		when(mockResponseFuture.handle(GithubApiClientImpl.RESPONSE_HANDLER)).thenReturn(mockResponseFuture);
		when(mockResponseFuture.thenApply(GithubApiClientImpl.RELEASE_MAPPER)).thenReturn(mockReleaseFuture);
		when(mockReleaseFuture.get()).thenThrow(ex);		
		
		assertEquals(ex, assertThrows(GithubApiException.class, () -> {			
			// Call under test
			client.getRelease(org, repo, "someTag");
		}).getCause());
	}
	
	@Test
	public void testResponseHandler() {
		when(mockResponse.statusCode()).thenReturn(HttpStatus.OK.value());
		Throwable ex = null;
		
		// Call under test
		assertEquals(mockResponse, GithubApiClientImpl.RESPONSE_HANDLER.apply(mockResponse, ex));
	}
	
	@Test
	public void testResponseHandlerWithUexpectedStatus() {
		when(mockResponse.statusCode()).thenReturn(HttpStatus.NOT_FOUND.value());
		when(mockResponse.body()).thenReturn("body");
		
		Throwable ex = null;
		
		// Call under test
		assertEquals("The request failed with status 404 NOT_FOUND (Response: body).", assertThrows(GithubApiException.class, () -> {
			// Call under test
			GithubApiClientImpl.RESPONSE_HANDLER.apply(mockResponse, ex);
		}).getMessage());
	}
	
	@Test
	public void testResponseHandlerWithException() {
		
		Throwable ex = new RuntimeException("failed");
		
		assertEquals(ex, assertThrows(GithubApiException.class, () -> {
			// Call under test
			GithubApiClientImpl.RESPONSE_HANDLER.apply(mockResponse, ex);
		}).getCause());
	}
	
	@Test
	public void testReleaseMapper() {
		
		when(mockResponse.body()).thenReturn("{\"id\": 123, \"tag_name\": \"1.0.0\", \"name\": \"release\", \"url\": \"https://release.url\"}");
		
		Release expected = new Release().setId(123L).setName("release").setTag_name("1.0.0").setUrl("https://release.url");
		
		assertEquals(expected, GithubApiClientImpl.RELEASE_MAPPER.apply(mockResponse));
	}
	
	@Test
	public void testReleaseMapperWithMalformedJson() {
		
		when(mockResponse.body()).thenReturn("malformed");
		
		GithubApiException ex = assertThrows(GithubApiException.class, () -> {
			GithubApiClientImpl.RELEASE_MAPPER.apply(mockResponse);
		});
		
		assertTrue(ex.getCause() instanceof JSONObjectAdapterException);
		assertEquals("org.sagebionetworks.schema.adapter.JSONObjectAdapterException: org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]", ex.getMessage());
	}
	
	private HttpRequest expectedReleaseRequest(String releaseTag) {
		return HttpRequest.newBuilder()
			.timeout(Duration.of(5, ChronoUnit.SECONDS))
			.GET()
			.uri(URI.create("https://api.github.com/repos/" + org + "/" + repo + "/releases/" + releaseTag))
			.headers(
				HttpHeaders.USER_AGENT, "Synapse-Org",
				HttpHeaders.ACCEPT, "application/vnd.github+json",
				"X-GitHub-Api-Version", "2022-11-28")
			.build();
	}
}
