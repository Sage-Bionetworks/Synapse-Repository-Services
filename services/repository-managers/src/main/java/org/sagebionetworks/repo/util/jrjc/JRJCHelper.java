package org.sagebionetworks.repo.util.jrjc;


import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;

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
        BasicIssue createdIssue = createIssue(jiraClient, JIRA_FLAG_ISSUE_TYPE_NAME, FLAG_SUMMARY, params);
        return createdIssue.getKey();
	}
	
	public static String createRestrictIssue(JiraClient jiraClient, String principalId, String displayName, String dataObjectId) {
        Map<String,String> params = new HashMap<String,String>();
        params.put(JIRA_PRINCIPAL_ID_ISSUE_FIELD_NAME, principalId);
        params.put(JIRA_USER_DISPLAY_NAME_ISSUE_FIELD_NAME, displayName);
        params.put(JIRA_SYNAPSE_ENTITY_ID_FIELD_NAME, dataObjectId);
        BasicIssue createdIssue = createIssue(jiraClient, JIRA_RESTRICT_ISSUE_TYPE_NAME, RESTRICT_SUMMARY, params);
        return createdIssue.getKey();
	} 
	
	/**
	 * 
	 * @param client
	 * @param issueTypeName
	 * @param params a map from field names to field values
	 * @return
	 */
	public static BasicIssue createIssue(JiraClient jiraClient, String issueTypeName, String summary, Map<String,String> params) {
		// first, find the project from the JIRA_PROJECT_KEY
        Project project = jiraClient.getProject(JIRA_PROJECT_KEY);
        
        // second, find the issue type ID from the issue type name
		long issueTypeId = -1L;
		for (IssueType it : project.getIssueTypes()) {
			if (issueTypeName.equalsIgnoreCase(it.getName())) issueTypeId = it.getId();
		}
		if (issueTypeId==-1L) throw new IllegalStateException("Cannot find issue type "+issueTypeName+" in Jira project "+JIRA_PROJECT_KEY);
		
		// third, find the defined fields, mapping their names to their IDs	
		Map<String, String> lcFieldNameToIdMap = new HashMap<String, String>();
		for (Field field : jiraClient.getFields()) {
			lcFieldNameToIdMap.put(field.getName().toLowerCase(), field.getId());
		}

        // now start building the issue.  first set the project and issue type
    	IssueInputBuilder builder =  new IssueInputBuilder(JIRA_PROJECT_KEY, issueTypeId);
    	
    	// now set the summary
        builder.setSummary(summary);
        
        // now fill in the fields
        for (String fieldName : params.keySet()) {
        	String fieldId = lcFieldNameToIdMap.get(fieldName.toLowerCase());
            if (fieldId==null) throw new IllegalStateException("Cannot find field named "+fieldName.toLowerCase()+
            		" fields are: "+lcFieldNameToIdMap.keySet());
            builder.setFieldValue(fieldId, params.get(fieldName));
        }
        
        IssueInput issueInput = builder.build();
        
        // finally, create the issue
        return jiraClient.createIssue(issueInput);
	}

}
