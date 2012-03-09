package org.sagebionetworks.workflow;

import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;

/**
 * FlowBlockJUnit4ClassRunner tests do not support external clients
 * 
 * @author deflaux
 *
 */
public class MockSageCommonsChildWorkflowDispatcher implements
		SageCommonsChildWorkflowDispatcher {

	private static final Logger log = Logger
	.getLogger(MockSageCommonsChildWorkflowDispatcher.class.getName());
	
	private SageCommonsRScriptWorkflowClientFactory clientFactory;

	/**
	 * Constructor for regular unit tests
	 */
	public MockSageCommonsChildWorkflowDispatcher() {
		this.clientFactory = null;
	}

	/**
	 * Constructor for FlowBlockJUnit4ClassRunner tests
	 * 
	 * @param clientFactory
	 */
	public MockSageCommonsChildWorkflowDispatcher(SageCommonsRScriptWorkflowClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	public void dispatchRScriptChildWorkflow(String script,
			String spreadsheetData) {
		
		if(null == clientFactory) {
			return;
		}
		
		SageCommonsRScriptWorkflowClient childWorkflow = clientFactory
				.getClient();
		childWorkflow.runRScript(SageCommonsConfigHelper.getWorkflowScript(),
				spreadsheetData);

		WorkflowExecution workflowExecution = childWorkflow
				.getWorkflowExecution();
		log.debug("Started runRScript workflow with workflowId=\""
				+ workflowExecution.getWorkflowId() + "\" and runId=\""
				+ workflowExecution.getRunId() + "\"");
	}

}
