package org.sagebionetworks.repo.manager.agent.handler;

import com.google.api.gax.rpc.ApiException;

/**
 * A handler that provides more information to an Agent when an invoke_agent
 * call return with "return control" response. The provided action group and
 * function identify which function this handler handles.
 */
public interface ReturnControlHandler {

	/**
	 * Must match the name of the action group defined for this function.
	 * 
	 * @return
	 */
	String getActionGroup();

	/**
	 * Must match the name of the function within the action group.
	 * 
	 * @return
	 */
	String getFunction();
	
	/**
	 * Does this handler need write access to execute?
	 * @return
	 */
	boolean needsWriteAccess();

	/**
	 * Called when an invoke_agent request returns with a "return control" response
	 * that matches this handler's action group and function. The provide event will
	 * contain any parameter from the agent, to be used to handle this event.
	 * 
	 * @param event
	 * @return
	 * @throws ApiException
	 */
	String handleEvent(ReturnControlEvent event) throws Exception;
}
