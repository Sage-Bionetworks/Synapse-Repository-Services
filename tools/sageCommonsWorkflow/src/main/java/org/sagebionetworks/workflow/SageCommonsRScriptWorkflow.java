package org.sagebionetworks.workflow;

import org.sagebionetworks.workflow.Constants;

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;

/**
 * @author deflaux
 *
 */
@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS,
		defaultTaskList = SageCommonsRScriptWorkflow.DECISIONS_TASK_LIST) 
public interface SageCommonsRScriptWorkflow {
	
	/**
	 * The one and only task list to use for decisions in this workflow
	 */
	static final String DECISIONS_TASK_LIST = "decisions";
	
    /**
     * @param script
     * @param spreadsheetData
     */
    @Execute(version = SageCommonsActivities.VERSION) 
    void runRScript(String script, String spreadsheetData);

}