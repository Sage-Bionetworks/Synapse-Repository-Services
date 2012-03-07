package org.sagebionetworks.workflow;

import org.sagebionetworks.workflow.Constants;

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;
import com.amazonaws.services.simpleworkflow.model.ChildPolicy;

@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = Constants.ONE_DAY_OF_SECONDS,
		defaultTaskList = SageCommonsWorkflow.DECISIONS_TASK_LIST,
		defaultChildPolicy = ChildPolicy.TERMINATE) 
public interface SageCommonsWorkflow {
	
	/**
	 * The one and only task list to use for decisions in this workflow
	 */
	static final String DECISIONS_TASK_LIST = "decisions";
	
    @Execute(version = SageCommonsActivities.VERSION) 
    void processSubmission(String submission);

}