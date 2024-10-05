package org.sagebionetworks.repo.util.github;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.sagebionetworks.repo.model.utils.github.Release;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GithubApiClientImpl implements GithubApiClient {
	
	private static final String[] GITHUB_REQUEST_HEADERS = new String[] {
		HttpHeaders.USER_AGENT, "Synapse-Org-App",
		HttpHeaders.ACCEPT, "application/vnd.github+json",
		"X-GitHub-Api-Version", "2022-11-28"
	};
	
	private static final Duration TIMEOUT = Duration.of(5, ChronoUnit.SECONDS);
	
	private static final String GITHUB_ENDPOINT = "https://api.github.com/";
	
	private static final String API_RELEASE = GITHUB_ENDPOINT + "repos/%s/%s/releases/%s";
	
	private static HttpRequest buildGetRequest(String uri) {
		return HttpRequest.newBuilder()
			.timeout(TIMEOUT)
			.GET()
			.uri(URI.create(uri))
			.headers(GITHUB_REQUEST_HEADERS)
			.build();
	}
	
	static BodyHandler<String> BODY_HANDLER = BodyHandlers.ofString();
	
	static BiFunction<HttpResponse<String>, Throwable, HttpResponse<String>> RESPONSE_HANDLER = (response, ex) -> {
		if (ex != null) {
			throw new GithubApiException(ex);
		}
		
		HttpStatus status = HttpStatus.resolve(response.statusCode());
		
		if (!HttpStatus.OK.equals(status)) {
			throw new GithubApiException("The request failed.", status, response.body());
		}
		
		return response;
	};
	
	static Function<HttpResponse<String>, Release> RELEASE_MAPPER = (response) -> {
		try {
			return EntityFactory.createEntityFromJSONString(response.body(), Release.class);
		} catch (JSONObjectAdapterException e) {
			throw new GithubApiException(e);
		}
	};
	
	private HttpClient httpClient;
	
	public GithubApiClientImpl(HttpClient defaultHttpClient) {
		this.httpClient = defaultHttpClient;
	}

	@Override
	public Release getLatestRelease(String org, String repository) {
		return getRelease(org, repository, "latest");
	}
	
	@Override
	public Release getReleaseByTag(String org, String repository, String tagName) {
		return getRelease(org, repository, "tags/" + tagName);
	}
	
	Release getRelease(String org, String repository, String releaseTag) {
		HttpRequest request = buildGetRequest(String.format(API_RELEASE, org, repository, releaseTag));

		CompletableFuture<Release> future = httpClient
			.sendAsync(request, BODY_HANDLER)
			.handle(RESPONSE_HANDLER)
			.thenApply(RELEASE_MAPPER);
		
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new GithubApiException(e);
		}
	}

}
