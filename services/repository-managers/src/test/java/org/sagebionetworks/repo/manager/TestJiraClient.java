package org.sagebionetworks.repo.manager;

import java.net.URI;

import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.util.jrjc.JiraClientImpl;

import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;

/**
 * This is a test Jira client which avoids creating any actual issues in Jira
 * 
 * @author brucehoff
 *
 */
public class TestJiraClient implements JiraClient {
	private JiraClient innerJiraClient = new JiraClientImpl();
	
	@Override
	public Project getProject(String projectKey) {
		return innerJiraClient.getProject(projectKey);
	}

	@Override
	public Iterable<Field> getFields() {
		return innerJiraClient.getFields();
	}

	/*
	 * Note:  Here's where we avoid creating an actual issue
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.util.jrjc.JiraClient#createIssue(com.atlassian.jira.rest.client.api.domain.input.IssueInput)
	 */
	@Override
	public BasicIssue createIssue(IssueInput issueInput) {
		BasicIssue result = new BasicIssue(URI.create("/foo/bar/bas"), "test-key", 999L);
		return result;
	}

}
