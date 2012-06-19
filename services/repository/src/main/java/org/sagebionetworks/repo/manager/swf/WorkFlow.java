package org.sagebionetworks.repo.manager.swf;

import java.util.List;

import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowType;

/**
 * Any work flow should implement this interface.
 * It binds all of the parts of a work flow together,
 * and is used by the SimpleWorkFlowRegister to register
 * the work flow and all of its parts.
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
	public RegisterWorkflowTypeRequest getWorkFlowTypeRequest();
	
	/**
	 * The type of this work flow.
	 * @return
	 */
	public WorkflowType getType();
	
	/**
	 * The decider used by this work flow.
	 * @return
	 */
	public Decider getDecider();
	
	/**
	 * Get the list of Activities.
	 * 
	 * @return
	 */
	public List<Activity> getActivityList();

}
