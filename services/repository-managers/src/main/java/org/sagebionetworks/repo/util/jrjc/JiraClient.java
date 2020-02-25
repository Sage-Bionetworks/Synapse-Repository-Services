package org.sagebionetworks.repo.util.jrjc;

import org.json.simple.JSONObject;
import java.util.Map;

/**
 * Mini JIRA REST API client
 */
public interface JiraClient {
	
	JSONObject getProjectInfo(String projectKey, String issueTypeName);
	Map<String,String> getFields();
	JSONObject createIssue(JSONObject issue);

}
