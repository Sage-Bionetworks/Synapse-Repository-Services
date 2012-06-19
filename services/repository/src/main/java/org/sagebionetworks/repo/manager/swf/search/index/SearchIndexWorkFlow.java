package org.sagebionetworks.repo.manager.swf.search.index;

import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.swf.Activity;
import org.sagebionetworks.repo.manager.swf.Decider;
import org.sagebionetworks.repo.manager.swf.WorkFlow;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.ChildPolicy;
import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import com.amazonaws.services.simpleworkflow.model.Run;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.amazonaws.services.simpleworkflow.model.WorkflowType;

/**
 * This work flow manages the entire life-cycle of an AWS Cloud Search Index.
 * A new work flow is started to create a Search Index.
 * A new application instance can discover an existing Search Index for its stack
 * by querying for the work flow's history.  Finally, when a Search Index is no
 * longer used by any instances it can be deleted.
 * 
 * @author John
 *
 */
public class SearchIndexWorkFlow implements WorkFlow {
	
	public static final String VERSION = "1.1.4";
	/**
	 * This is the task list used by the decider for this work flow.
	 */
	public static final TaskList DECIDER_TASK_LIST = new TaskList().withName(SearchIndexWorkFlow.class.getName()+"-"+VERSION);
	public static int DEFULT_EXECUTION_START_TO_CLOSE = 60*60*24*7; // one week (seconds)
	public static int DEFAULT_TASK_START_TO_CLOSE = 5*60; // 5 Mins
	
	@Autowired
	AmazonSimpleWorkflowClient simpleWorkFlowClient;
	
	private Decider decider;
	private List<Activity> activityList;

	@Override
	public RegisterWorkflowTypeRequest getWorkFlowTypeRequest() {
		WorkflowType type = getType();
		RegisterWorkflowTypeRequest request = new RegisterWorkflowTypeRequest();
		request.setName(type.getName());
		request.setVersion(type.getVersion());
		request.setDefaultTaskList(DECIDER_TASK_LIST);
		request.setDefaultExecutionStartToCloseTimeout(""+DEFULT_EXECUTION_START_TO_CLOSE);
		request.setDefaultTaskStartToCloseTimeout(""+DEFAULT_TASK_START_TO_CLOSE);
		request.setDefaultChildPolicy(ChildPolicy.REQUEST_CANCEL);
		request.setDescription("This work flow will create an AWS Cloud Search Index for a stack and then delete the index when it is not longer being used.");
		// This work flow uses the stack as a domain.
		request.setDomain(StackConfiguration.getStack());
		return request;
	}
	

	public Decider getDecider() {
		return decider;
	}


	public void setDecider(Decider decider) {
		this.decider = decider;
	}


	@Override
	public List<Activity> getActivityList() {
		return activityList;
	}

	public void setActivityList(List<Activity> activityList) {
		this.activityList = activityList;
	}

	@Override
	public WorkflowType getType() {
		WorkflowType type = new WorkflowType();
		type.setName(SearchIndexWorkFlow.class.getName());
		type.setVersion(VERSION);
		return type;
	}
	
	/**
	 * Start this work flow.
	 * @return
	 */
	public Run startWorkFlow(){
		// Build up the request.
		StartWorkflowExecutionRequest request = new StartWorkflowExecutionRequest();
		request.setWorkflowType(getType());
		request.setWorkflowId(StackConfiguration.getStackInstance());
		request.setDomain(StackConfiguration.getStack());
		return simpleWorkFlowClient.startWorkflowExecution(request);
	}

}
