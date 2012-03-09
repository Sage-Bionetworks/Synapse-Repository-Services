package org.sagebionetworks.workflow;

import org.apache.log4j.Logger;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;

/**
 * This class is intended for activities that need to kick off workflows because
 * they must use an external client
 * 
 * @author deflaux
 * 
 */
public class SageCommonsChildWorkflowDispatcherImpl implements
		SageCommonsChildWorkflowDispatcher {

	private static final Logger log = Logger
			.getLogger(SageCommonsChildWorkflowDispatcherImpl.class.getName());

	private SageCommonsRScriptWorkflowClientExternalFactory clientFactory = null;

	/**
	 * Normal constructor
	 */
	public SageCommonsChildWorkflowDispatcherImpl() {
		// Get a factory for these child workflows
		AmazonSimpleWorkflow swfService = SageCommonsConfigHelper
				.getSWFClient();
		String domain = SageCommonsConfigHelper.getStack();
		this.clientFactory = new SageCommonsRScriptWorkflowClientExternalFactoryImpl(
				swfService, domain);
	}

	@Override
	public void dispatchRScriptChildWorkflow(String script,
			String spreadsheetData) {
		SageCommonsRScriptWorkflowClientExternal childWorkflow = clientFactory
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
