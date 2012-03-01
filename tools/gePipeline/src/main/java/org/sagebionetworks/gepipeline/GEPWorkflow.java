package org.sagebionetworks.gepipeline;

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions;

/**
 * 
 */
@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = GEPWorkflow.MAX_WORKFLOW_TIMEOUT_HOURS * 3600, 
		defaultTaskList = GEPWorkflow.DECISIONS_TASK_LIST)
public interface GEPWorkflow {

	/**
	 * Workflow and Activity Annotations Constants
	 * 
	 * Dev Note: annotation values must be resolvable at compilation time, not
	 * runtime, therefore we cannot move this into a config file
	 */
	static final String WORKFLOW_NAME = "MetaGenomics";
	static final String VERSION = "1.2";

	static final String DECISIONS_TASK_LIST = WORKFLOW_NAME + "Decisions";

	static final String ACTIVITY_REQUIREMENT_SMALL = "Small";
	static final String ACTIVITY_REQUIREMENT_MEDIUM = "Medium";
	static final String ACTIVITY_REQUIREMENT_LARGE = "Large";
	static final String ACTIVITY_REQUIREMENT_EXTRA_LARGE = "ExtraLarge";
	
	static final String SMALL_ACTIVITY_TASK_LIST = WORKFLOW_NAME + ACTIVITY_REQUIREMENT_SMALL;
	static final String MEDIUM_ACTIVITY_TASK_LIST = WORKFLOW_NAME + ACTIVITY_REQUIREMENT_MEDIUM;
	static final String LARGE_ACTIVITY_TASK_LIST = WORKFLOW_NAME + ACTIVITY_REQUIREMENT_LARGE;
	static final String EXTRA_LARGE_ACTIVITY_TASK_LIST = WORKFLOW_NAME + ACTIVITY_REQUIREMENT_EXTRA_LARGE;

	static final int MAX_WORKFLOW_TIMEOUT_HOURS = 24;

	public static final String INPUT_DATA_PARAMETER_KEY = "--inputData";

	@Execute(name = GEPWorkflow.WORKFLOW_NAME, version = GEPWorkflow.VERSION)
	void runMetaGenomicsPipeline(String activityInput,
			String activityRequirement);
}
