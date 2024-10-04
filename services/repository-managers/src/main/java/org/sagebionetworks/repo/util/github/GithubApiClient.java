package org.sagebionetworks.repo.util.github;

import org.sagebionetworks.repo.model.utils.github.Release;

public interface GithubApiClient {

	Release getLatestRelease(String org, String repository);
	
	Release getReleaseByTag(String org, String repository, String tagName);
}
