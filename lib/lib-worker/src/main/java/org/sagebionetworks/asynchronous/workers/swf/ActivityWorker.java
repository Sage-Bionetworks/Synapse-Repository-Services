package org.sagebionetworks.asynchronous.workers.swf;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.amazonaws.services.simpleworkflow.model.PollForActivityTaskRequest;

/**
 * Worker for an Activity task.
 * @author John
 *
 */
public class ActivityWorker implements Runnable {
	
	Activity activity;
	ActivityTask activityTask;
	PollForActivityTaskRequest activityTaskRequest;
	AmazonSimpleWorkflowClient simpleWorkFlowClient;
	
	/**
	 * All params must be provided.
	 * 
	 * @param instance
	 * @param task
	 * @param pfdar
	 * @param simpleWorkFlowClient
	 */
	public ActivityWorker(Activity instance, ActivityTask task,
			PollForActivityTaskRequest pfdar,
			AmazonSimpleWorkflowClient simpleWorkFlowClient) {
		super();
		if(instance == null) throw new IllegalArgumentException("Activity cannot be null");
		if(task == null) throw new IllegalArgumentException("ActivityTask cannot be null");
		if(pfdar == null) throw new IllegalArgumentException("PollForActivityTaskRequest cannot be null");
		if(simpleWorkFlowClient == null) throw new IllegalArgumentException("AmazonSimpleWorkflowClient cannot be null");
		this.activity = instance;
		this.activityTask = task;
		this.activityTaskRequest = pfdar;
		this.simpleWorkFlowClient = simpleWorkFlowClient;
	}



	@Override
	public void run() {
		// make the call
		this.activity.doWork(activityTask, activityTaskRequest, simpleWorkFlowClient);
	}

}
