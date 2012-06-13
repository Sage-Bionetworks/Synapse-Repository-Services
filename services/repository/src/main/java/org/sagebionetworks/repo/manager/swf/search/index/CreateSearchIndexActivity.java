package org.sagebionetworks.repo.manager.swf.search.index;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.swf.Activity;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.amazonaws.services.simpleworkflow.model.PollForActivityTaskRequest;
import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;

/**
 * When this activity is run it will create a new Search Index.
 * 
 * @author John
 *
 */
public class CreateSearchIndexActivity implements Activity {
	
	public static final String VERSION = "1.0";

	@Override
	public TaskList getTaskList() {
		// TODO Auto-generated method stub
		return new TaskList().withName(CreateSearchIndexActivity.class.getName()+"-"+VERSION);
	}

	@Override
	public String getDomainName() {
		// this works on the base stack name
		return StackConfiguration.getStack();
	}

	@Override
	public void doWork(ActivityTask task, PollForActivityTaskRequest pfdar,
			AmazonSimpleWorkflowClient simpleWorkFlowClient) {
		// TODO Auto-generated method stub

	}

	@Override
	public RegisterActivityTypeRequest getRegisterRequest() {
		RegisterActivityTypeRequest request = new RegisterActivityTypeRequest();
		request.setDomain(getDomainName());
		request.setName(CreateSearchIndexActivity.class.getName());
		request.setVersion(VERSION);
		request.setDefaultTaskList(getTaskList());
		request.setDescription("An Activity that will create a new AWS Search Index");
		return request;
	}

}
