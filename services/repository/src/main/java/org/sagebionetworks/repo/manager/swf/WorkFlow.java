package org.sagebionetworks.repo.manager.swf;

import java.util.List;

import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;

/**
 * Any work flow should implement this interface.
 * It binds all of the parts of a work flow together,
 * and is used by the SimpleWorkFlowRegister to register
 * the workflow and all of its parts.
 * 
 * @author John
 *
 */
public interface WorkFlow {
	
	/**
	 * Information need to register this work flow with SWF.
	 * 
	 * @return
	 */
	public RegisterWorkflowTypeRequest getWorkFlowTypeRequest(String domainName);
	
	/**
	 * Get the list of deciders.
	 * @return
	 */
	public List<Decider> getDeciderList();
	
	/**
	 * Get the list of Activities.
	 * 
	 * @return
	 */
	public List<Activity> getActivityList();

}
