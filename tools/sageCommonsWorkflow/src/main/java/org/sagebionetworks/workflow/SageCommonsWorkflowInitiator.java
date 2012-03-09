package org.sagebionetworks.workflow;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;

/**
 * @author deflaux
 * 
 */
public class SageCommonsWorkflowInitiator {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// Create the client for Simple Workflow Service
		AmazonSimpleWorkflow swfService = SageCommonsConfigHelper
				.getSWFClient();
		String domain = SageCommonsConfigHelper.getStack();

		SageCommonsWorkflowClientExternalFactory clientFactory = new SageCommonsWorkflowClientExternalFactoryImpl(
				swfService, domain);

		SageCommonsWorkflowClientExternal workflow = clientFactory.getClient();
		workflow.processSubmission(args[0]);

		WorkflowExecution workflowExecution = workflow.getWorkflowExecution();
		System.out.println("Kicked off workflow for " + args[0]
				+ " with workflowId=\"" + workflowExecution.getWorkflowId()
				+ "\" and runId=\"" + workflowExecution.getRunId() + "\"");

		System.exit(0);
	}

}
