package org.sagebionetworks.repo.util.jrjc;

import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;

/**
 * This interface defines a wrapper around Jira's Rest Java Client to facilitate testing
 * 
 * @author brucehoff
 *
 */
public interface JiraClient {
	
	/**
	 * Given a project key, return the project
	 * @param projectKey
	 * @return
	 */
	Project getProject(String projectKey);
	
	/**
	 * return the issue fields defined for the Jira instance
	 * @return
	 */
	Iterable<Field> getFields();
	
	/**
	 * Create an issue in Jira, given an object containing all the information.
	 * 
	 * @param issueInput
	 * @return
	 */
	BasicIssue createIssue(IssueInput issueInput);
}
