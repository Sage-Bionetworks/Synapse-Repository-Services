package org.sagebionetworks.repo.manager.swf.search.index;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.manager.swf.Activity;
import org.sagebionetworks.repo.manager.swf.Decider;
import org.sagebionetworks.repo.manager.swf.WorkFlow;

import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;

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
	
	public static String version = "1.0";

	@Override
	public RegisterWorkflowTypeRequest getWorkFlowTypeRequest(String domainName) {
		RegisterWorkflowTypeRequest request = new RegisterWorkflowTypeRequest();
		request.setName(SearchIndexWorkFlow.class.getName());
		request.setVersion(version);
		request.setDefaultTaskList(getDefaultTaskList());
		request.setDescription("This work flow will either create a new search index or get the information of an exisitng search index");
		request.setDomain(domainName);
		return request;
	}
	
	/**
	 * What is the default task list for this domain?
	 * @return
	 */
	public TaskList getDefaultTaskList(){
		return new TaskList().withName(SearchIndexWorkFlow.class.getName()+"-"+version);
	}

	@Override
	public List<Decider> getDeciderList() {
		List<Decider> deciders = new LinkedList<Decider>();
		// This is the one and only decider for this work flow.
		deciders.add(new SearchIndexDecider());
		return deciders;
	}

	@Override
	public List<Activity> getActivityList() {
		List<Activity> activityList = new LinkedList<Activity>();
		// This activity creates a search index
		activityList.add(new CreateSearchIndexActivity());
		return activityList;
	}

}
