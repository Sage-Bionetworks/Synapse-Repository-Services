package org.sagebionetworks.repo.util.jrjc;

import org.json.simple.JSONObject;
import java.util.Map;

/**
 * Mini JIRA REST API client
 */
public interface JiraClient {
	
	JSONObject getProjectInfo(String projectKey, String issueTypeName) throws JiraClientException;
	Map<String,String> getFields()  throws JiraClientException;
	JSONObject createIssue(JSONObject issue)  throws JiraClientException;

}
