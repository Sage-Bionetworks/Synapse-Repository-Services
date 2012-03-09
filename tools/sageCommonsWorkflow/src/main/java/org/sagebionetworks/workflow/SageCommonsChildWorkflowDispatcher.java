package org.sagebionetworks.workflow;

/**
 * This interface was needed to support unit testing of the workflow since for this workflow the activities call additional workflows
 * 
 * @author deflaux
 *
 */
public interface SageCommonsChildWorkflowDispatcher {
	
	/**
	 * @param script
	 * @param spreadsheetData
	 */
	void dispatchRScriptChildWorkflow(String script, String spreadsheetData);

}
