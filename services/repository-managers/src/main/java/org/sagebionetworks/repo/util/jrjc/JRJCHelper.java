package org.sagebionetworks.repo.util.jrjc;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

public class JRJCHelper {

	private static final String JIRA_PROJECT_KEY = "SG"; // dev Synapse Governance, "SG" for "Synapse Governance"
	private static final String JIRA_FLAG_ISSUE_TYPE_NAME = "Flag";
	private static final String JIRA_RESTRICT_ISSUE_TYPE_NAME = "Access Restriction";
	private static final String JIRA_PRINCIPAL_ID_ISSUE_FIELD_NAME = "Synapse Principal ID"; // The ID of the Synapse user reporting the issue.
	private static final String JIRA_USER_DISPLAY_NAME_ISSUE_FIELD_NAME = "Synapse User Display Name"; // The display name of the Synapse user reporting the issue.
	private static final String JIRA_SYNAPSE_ENTITY_ID_FIELD_NAME = "Synapse Data Object"; // The ID of the Synapse object which is the subject of the issue.
	private static final String FLAG_SUMMARY = "Request for ACT to review data";
	private static final String RESTRICT_SUMMARY = "Request for ACT to add data restriction";
	
	public static String createFlagIssue(JiraClient jiraClient, String principalId, String displayName, String dataObjectId) {
        Map<String,String> params = new HashMap<String,String>();
        params.put(JIRA_PRINCIPAL_ID_ISSUE_FIELD_NAME, principalId);
        params.put(JIRA_USER_DISPLAY_NAME_ISSUE_FIELD_NAME, displayName);
        params.put(JIRA_SYNAPSE_ENTITY_ID_FIELD_NAME, dataObjectId);
        JSONObject createdIssue = createIssue(jiraClient, JIRA_FLAG_ISSUE_TYPE_NAME, FLAG_SUMMARY, params);
		return (String) createdIssue.get("key");
	}
	
	public static String createRestrictIssue(JiraClient jiraClient, String principalId, String displayName, String dataObjectId) {
        Map<String,String> params = new HashMap<String,String>();
        params.put(JIRA_PRINCIPAL_ID_ISSUE_FIELD_NAME, principalId);
        params.put(JIRA_USER_DISPLAY_NAME_ISSUE_FIELD_NAME, displayName);
        params.put(JIRA_SYNAPSE_ENTITY_ID_FIELD_NAME, dataObjectId);
        JSONObject createdIssue = createIssue(jiraClient, JIRA_RESTRICT_ISSUE_TYPE_NAME, RESTRICT_SUMMARY, params);
        return (String) createdIssue.get("key");
	} 
	
	/**
	 * 
	 * @param client
	 * @param issueTypeName
	 * @param params a map from field names to field values
	 * @return
	 */
	public static JSONObject createIssue(JiraClient jiraClient, String issueTypeName, String summary, Map<String,String> params) {
		JSONObject projectInfo = jiraClient.getProjectInfo(JIRA_PROJECT_KEY, issueTypeName);
		String projectId = (String) projectInfo.get("id");
		Long issueTypeId = (Long) projectInfo.get("issueTypeId");

		Map<String, String> fieldsMap = jiraClient.getFields();
		JSONObject issue = new JSONObject();
		JSONObject issueFields = new JSONObject();
		JSONObject o = new JSONObject();
		issueFields.put("summary", summary);
		o = new JSONObject();
		o.put("id", projectId);
		issueFields.put("project", o);
		o = new JSONObject();
		o.put("id", issueTypeId);
		issueFields.put("issuetype", o);
		for (String k: params.keySet()) {
			String fk = fieldsMap.get(k);
			issueFields.put(fk, params.get(k));
		}
		issue.put("fields", issueFields);
		JSONObject createdIssue = jiraClient.createIssue(issue);
		return createdIssue;
	}
}
