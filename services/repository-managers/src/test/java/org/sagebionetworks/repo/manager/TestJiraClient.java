package org.sagebionetworks.repo.manager;

import java.net.URI;
import java.util.Map;

import org.json.simple.JSONObject;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.util.jrjc.JiraClientImpl;

/**
 * This is a test Jira client which avoids creating any actual issues in Jira
 * 
 * @author brucehoff
 *
 */
public class TestJiraClient implements JiraClient {
	private JiraClient innerJiraClient = new JiraClientImpl();
	
	@Override
	public JSONObject getProjectInfo(String projectKey, String issueTypeName) {
		return innerJiraClient.getProjectInfo(projectKey, issueTypeName);
	}

	@Override
	public Map<String,String> getFields() {
		return innerJiraClient.getFields();
	}

	/*
	 * Note:  Here's where we avoid creating an actual issue
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.util.jrjc.JiraClient#createIssue(com.atlassian.jira.rest.client.api.domain.input.IssueInput)
	 */
	@Override
	public JSONObject createIssue(JSONObject issueInput) {
		//BasicIssue result = new BasicIssue(URI.create("/foo/bar/bas"), "test-key", 999L);
		return null;
	}

}
