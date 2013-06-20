package org.sagebionetworks.repo.util.jrjc;

import java.util.concurrent.ExecutionException;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.api.OptionalIterable;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.util.concurrent.Promise;

public class JiraClientImpl implements JiraClient {
	private JiraRestClient restClient;
	
	private static <T> T waitForPromise(Promise<T> promise) {
		try {
			while (!promise.isDone()) {
				Thread.sleep(100L);
			}
			return promise.get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public JiraClientImpl(JiraRestClient restClient) {
		this.restClient=restClient;
	}

	@Override
	public Project getProject(String projectKey) {
		ProjectRestClient prc = restClient.getProjectClient();
		return waitForPromise(prc.getProject(projectKey));
	}

	@Override
	public Iterable<Field> getFields() {
		// NOTE:  In order to appear in this list a Jira 'custom field' 
		// must have a 'global context'.  If it applies just to selected
		// projects it will not appear in this list.
    	MetadataRestClient mrc = restClient.getMetadataClient();
    	return waitForPromise(mrc.getFields());
	}

	@Override
	public BasicIssue createIssue(IssueInput issueInput) {
        IssueRestClient irc = restClient.getIssueClient();
        Promise<BasicIssue> basicIssuePromise = irc.createIssue(issueInput);
        return waitForPromise(basicIssuePromise);
	}

	@Override
	public Iterable<BasicProject> listProjects() {
		ProjectRestClient prc = restClient.getProjectClient();
		return waitForPromise(prc.getAllProjects());
	}

}
