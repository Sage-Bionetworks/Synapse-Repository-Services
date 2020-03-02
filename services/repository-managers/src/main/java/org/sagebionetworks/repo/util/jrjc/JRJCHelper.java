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
        CreatedIssue createdIssue = createIssue(jiraClient, JIRA_FLAG_ISSUE_TYPE_NAME, FLAG_SUMMARY, params);
		return (String) createdIssue.getKey();
	}
	
	public static String createRestrictIssue(JiraClient jiraClient, String principalId, String displayName, String dataObjectId) {
        Map<String,String> params = new HashMap<String,String>();
        params.put(JIRA_PRINCIPAL_ID_ISSUE_FIELD_NAME, principalId);
        params.put(JIRA_USER_DISPLAY_NAME_ISSUE_FIELD_NAME, displayName);
        params.put(JIRA_SYNAPSE_ENTITY_ID_FIELD_NAME, dataObjectId);
        CreatedIssue createdIssue = createIssue(jiraClient, JIRA_RESTRICT_ISSUE_TYPE_NAME, RESTRICT_SUMMARY, params);
        return (String) createdIssue.getKey();
	} 
	
	/**
	 * 
	 * @param client
	 * @param issueTypeName
	 * @param params a map from field names to field values
	 * @return
	 */
	public static CreatedIssue createIssue(JiraClient jiraClient, String issueTypeName, String summary, Map<String,String> params) {
		ProjectInfo projectInfo = jiraClient.getProjectInfo(JIRA_PROJECT_KEY, issueTypeName);
		String projectId = projectInfo.getProjectId();
		Long issueTypeId = projectInfo.getIssueTypeId();
		Map<String, String> fieldsMap = jiraClient.getFields();

		BasicIssue basicIssue = new BasicIssue();
		basicIssue.setProjectId(projectId);
		basicIssue.setIssueTypeId(issueTypeId);
		basicIssue.setSummary(summary);
		basicIssue.setCustomFields(mapParams(fieldsMap, params));

		CreatedIssue createdIssue = jiraClient.createIssue(basicIssue);
		return createdIssue;
	}

	private static Map<String, String> mapParams(Map<String, String>fieldsMap, Map<String, String> params) {
		Map<String, String> mapped = new HashMap<>();
		for (String k: params.keySet()) {
			String fk = fieldsMap.get(k);
			mapped.put(fk, params.get(k));
		}
		return mapped;
	}
}
