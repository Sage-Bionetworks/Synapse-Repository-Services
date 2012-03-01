package org.sagebionetworks.workflow.curation;

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
		defaultTaskList = TcgaWorkflow.DECISIONS_TASK_LIST) 
public interface TcgaWorkflow {
	
	/**
	 * The one and only task list to use for decisions in this workflow
	 */
	static final String DECISIONS_TASK_LIST = "TcgaDecisions";
	
    /**
     * @param layerId
     * @param tcgaUrl
     */
    @Execute(version = TcgaActivities.VERSION) 
    void addLocationToRawTcgaLayer(String layerId, String tcgaUrl);

}