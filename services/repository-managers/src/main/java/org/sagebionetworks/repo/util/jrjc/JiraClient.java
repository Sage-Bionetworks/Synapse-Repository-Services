package org.sagebionetworks.repo.util.jrjc;

import org.json.simple.JSONObject;
import java.util.Map;

/**
 * Mini JIRA REST API client
 */
public interface JiraClient {
	
	ProjectInfo getProjectInfo(String projectKey, String issueTypeName) throws JiraClientException;
	Map<String,String> getFields()  throws JiraClientException;
	CreatedIssue createIssue(BasicIssue issue)  throws JiraClientException;

}
