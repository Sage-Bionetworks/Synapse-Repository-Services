package org.sagebionetworks.asynchronous.workers.swf;

import java.util.List;

/**
 * Abstraction for the AWS Simple Work Flow registration process.
 * @author John
 *
 */
public interface SimpleWorkFlowRegister {
	

	/**
	 * Get all of the registered work flows.
	 * 
	 * @return
	 */
	public List<WorkFlow> getWorkFlowList();
	/**
	 * Get the list of registered tasks.
	 * 
	 * @return
	 */
	public List<Task> getTaskList();

}
