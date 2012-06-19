package org.sagebionetworks.repo.manager.swf;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.amazonaws.services.simpleworkflow.model.ActivityType;
import com.amazonaws.services.simpleworkflow.model.PollForActivityTaskRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest;

/**
 * An abstraction for an Activity worker.
 * 
 * @author John
 *
 */
public interface Activity extends Task {
	
	/**
	 * This is used to register a new Activity type.
	 * @return
	 */
	public RegisterActivityTypeRequest getRegisterRequest();
	
	/**
	 * This is used to start an activity of this type.
	 * @return
	 */
	public ActivityType getActivityType();
	
	/**
	 * This method is called when there is work for this activity to perform.
	 * @param task - The results from the last poll.
	 * @param pfdtr - The request used to generate the ActivityTask parameter.
	 * @param simpleWorkFlowClient - The AmazonSimpleWorkflowClient need report work status.
	 */
	public void doWork(ActivityTask task, PollForActivityTaskRequest pfdar,	AmazonSimpleWorkflowClient simpleWorkFlowClient);

}
