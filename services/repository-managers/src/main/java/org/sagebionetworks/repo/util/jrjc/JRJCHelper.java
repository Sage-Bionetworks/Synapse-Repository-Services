package org.sagebionetworks.repo.util.jrjc;


import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

public class JRJCHelper {

	private static final String JIRA_URL = "https://sagebionetworks.jira.com";
	private static final String SYNAPSE_JIRA_USER = "synapse-jira-service";
	private static final String SYNAPSE_JIRA_PASSWORD = "5ec8800858e5cdbe2760a4";
	private static final String JIRA_PROJECT_KEY = "DSG"; // dev Synapse Governance, "SG" for "Synapse Governance"
	private static final String JIRA_FLAG_ISSUE_TYPE_NAME = "Flag";
	private static final String JIRA_RESTRICT_ISSUE_TYPE_NAME = "Access Restriction";
	private static final String JIRA_PRINCIPAL_ID_ISSUE_FIELD_NAME = "Synapse Principal ID"; // The ID of the Synapse user reporting the issue.
	private static final String JIRA_USER_DISPLAY_NAME_ISSUE_FIELD_NAME = "Synapse User Display Name"; // The display name of the Synapse user reporting the issue.
	private static final String JIRA_SYNAPSE_ENTITY_ID_FIELD_NAME = "Synapse Data Object"; // The ID of the Synapse object which is the subject of the issue.
	private static final String FLAG_SUMMARY = "Request for ACT to review data";
	private static final String RESTRICT_SUMMARY = "Request for ACT to add data restriction";
	
	/**
	 * mape the field names to their ids, making the names lower case for case insensitivity
	 * @param fields
	 * @return
	 */
	public static Map<String, String> lcFieldNameToIdMap(Iterable<Field> fields) {
		Map<String, String> ans = new HashMap<String, String>();
		for (Field field : fields) {
			ans.put(field.getName().toLowerCase(), field.getId());
		}
		return ans;
	}
	
	private static JiraClient getJiraClientInstance() {
    	final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
    	URI jiraServerUri = URI.create(JIRA_URL);
    	JiraRestClient jrc = factory.createWithBasicHttpAuthentication(jiraServerUri, SYNAPSE_JIRA_USER, SYNAPSE_JIRA_PASSWORD);
    	JiraClientImpl jiraClientImpl = new JiraClientImpl(jrc);
    	return jiraClientImpl;
	}
	
	public static void main(String[] args) throws Exception {
		JiraClient jc = getJiraClientInstance();
    	System.out.println(createRestrictIssue(jc, "1010101", "foo@bar.com", "syn123456"));
	}
        
	public static String createFlagIssue(JiraClient jiraClient, String principalId, String displayName, String dataObjectId)  throws ExecutionException, InterruptedException {
        Map<String,String> params = new HashMap<String,String>();
        params.put(JIRA_PRINCIPAL_ID_ISSUE_FIELD_NAME, principalId);
        params.put(JIRA_USER_DISPLAY_NAME_ISSUE_FIELD_NAME, displayName);
        params.put(JIRA_SYNAPSE_ENTITY_ID_FIELD_NAME, dataObjectId);
        BasicIssue createdIssue = createIssue(jiraClient, JIRA_FLAG_ISSUE_TYPE_NAME, FLAG_SUMMARY, params);
        return JIRA_URL+"/browse/"+createdIssue.getKey();
	}
	
	public static String createRestrictIssue(JiraClient jiraClient, String principalId, String displayName, String dataObjectId)  throws ExecutionException, InterruptedException {
        Map<String,String> params = new HashMap<String,String>();
        params.put(JIRA_PRINCIPAL_ID_ISSUE_FIELD_NAME, principalId);
        params.put(JIRA_USER_DISPLAY_NAME_ISSUE_FIELD_NAME, displayName);
        params.put(JIRA_SYNAPSE_ENTITY_ID_FIELD_NAME, dataObjectId);
        BasicIssue createdIssue = createIssue(jiraClient, JIRA_RESTRICT_ISSUE_TYPE_NAME, RESTRICT_SUMMARY, params);
        return JIRA_URL+"/browse/"+createdIssue.getKey();
	} 
	
	
	/**
	 * 
	 * @param client
	 * @param issueTypeName
	 * @param params a map from field names to field values
	 * @return
	 */
	public static BasicIssue createIssue(JiraClient jiraClient, String issueTypeName, String summary, Map<String,String> params) throws ExecutionException, InterruptedException {
		// first, find the project from the JIRA_PROJECT_KEY
        Project project = jiraClient.getProject(JIRA_PROJECT_KEY);
        
        // second, find the issue type ID from the issue type name
		long issueTypeId = -1L;
		for (IssueType it : project.getIssueTypes()) {
			if (issueTypeName.equalsIgnoreCase(it.getName())) issueTypeId = it.getId();
		}
		if (issueTypeId==-1L) throw new IllegalStateException("Cannot find issue type "+issueTypeName+" in Jira project "+JIRA_PROJECT_KEY);
		
		// third, find the defined fields, mapping their names to their IDs
    	Map<String, String> lcFieldNameToIdMap = lcFieldNameToIdMap(jiraClient.getFields());

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
