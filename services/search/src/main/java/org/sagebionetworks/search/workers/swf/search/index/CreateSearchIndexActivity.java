package org.sagebionetworks.search.workers.swf.search.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.swf.Activity;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.amazonaws.services.simpleworkflow.model.ActivityType;
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
	
	static private Log log = LogFactory.getLog(CreateSearchIndexActivity.class);

	@Override
	public TaskList getTaskList() {
		return new TaskList().withName(CreateSearchIndexActivity.class.getName()+"-"+SearchIndexWorkFlow.VERSION);
	}

	@Override
	public String getDomainName() {
		// this works on the base stack name
		return StackConfiguration.getStack();
	}

	@Override
	public void doWork(ActivityTask task, PollForActivityTaskRequest pfdar,
			AmazonSimpleWorkflowClient simpleWorkFlowClient) {
		// First create Connect to Cloud search client
		if(log.isDebugEnabled()){
			log.debug("doWork() "+task);
		}

	}

	@Override
	public RegisterActivityTypeRequest getRegisterRequest() {
		RegisterActivityTypeRequest request = new RegisterActivityTypeRequest();
		request.setDomain(getDomainName());
		ActivityType type = getActivityType();
		request.setName(type.getName());
		request.setVersion(type.getVersion());
		request.setDefaultTaskList(getTaskList());
		request.setDescription("An Activity that will create a new AWS Search Index");
		return request;
	}

	@Override
	public ActivityType getActivityType() {
		return new ActivityType().withName(CreateSearchIndexActivity.class.getName()).withVersion(SearchIndexWorkFlow.VERSION);
		
	}

}
