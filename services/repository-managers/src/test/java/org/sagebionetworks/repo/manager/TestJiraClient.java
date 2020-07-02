package org.sagebionetworks.repo.manager;

import java.util.Map;

import org.json.simple.JSONObject;
import org.sagebionetworks.repo.util.jrjc.*;

/**
 * This is a test Jira client which avoids creating any actual issues in Jira
 * 
 * @author brucehoff
 *
 */
public class TestJiraClient implements JiraClient {
	private JiraClient innerJiraClient = new JiraClientImpl();
	
	@Override
	public ProjectInfo getProjectInfo(String projectKey, String issueTypeName) {
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
	public CreatedIssue createIssue(BasicIssue issueInput) {
		CreatedIssue createdIssue = new CreatedIssue();
		createdIssue.setId("9999");
		createdIssue.setKey("test-key");
		createdIssue.setUrl("https://someUrl");
		return createdIssue;
	}

}
