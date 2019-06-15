package org.sagebionetworks.repo.util.jrjc;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import io.atlassian.util.concurrent.Promise;
import org.sagebionetworks.StackConfigurationSingleton;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

/**
 * An implementation of JiraClient using the Jira-Rest-Java-Client
 * 
 * @author brucehoff
 *
 */
public class JiraClientImpl implements JiraClient {
	private static final String JIRA_URL = "https://sagebionetworks.jira.com";

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
	
	public JiraClientImpl() {
    	final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
    	URI jiraServerUri = URI.create(JIRA_URL);
    	this.restClient = factory.createWithBasicHttpAuthentication(
    			jiraServerUri, 
    			StackConfigurationSingleton.singleton().getJiraUserName(), 
    			StackConfigurationSingleton.singleton().getJiraUserPassword());
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


}
